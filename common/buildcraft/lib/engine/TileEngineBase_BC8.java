package buildcraft.lib.engine;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.biome.Biome;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.mj.*;
import buildcraft.api.tiles.IDebuggable;

import buildcraft.lib.block.VanillaRotationHandlers;
import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.net.PacketBufferBC;
import buildcraft.lib.tile.TileBC_Neptune;

public abstract class TileEngineBase_BC8 extends TileBC_Neptune implements ITickable, IDebuggable {

    /** Heat per {@link MjAPI#MJ}. */
    public static final double HEAT_PER_MJ = 0.0023;

    public static final double MIN_HEAT = 20;
    public static final double IDEAL_HEAT = 100;
    public static final double MAX_HEAT = 250;

    @Nonnull
    public final IMjConnector mjConnector = createConnector();
    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(mjConnector);

    protected double heat = MIN_HEAT;// TODO: sync gui data
    protected long power = 0;// TODO: sync gui data
    private long lastPower = 0;
    /** Increments from 0 to 1. Above 0.5 all of the held power is emitted. */
    private float progress, lastProgress;
    private int progressPart = 0;

    protected EnumPowerStage powerStage = EnumPowerStage.BLUE;
    protected EnumFacing currentDirection = EnumFacing.UP;

    public long currentOutput;// TODO: sync gui data
    public boolean isRedstonePowered = false;
    private boolean isPumping = false;

    // Needed: Power stored

    /* TODO: GuiDataValue<T> (new class) -- should automate sending values in TileBC_Neptune.NET_GUI_TICK it MUST be
     * nice to use! otherwise fallback to having private "last sent" values. Look at Container.detectAndSendChanges for
     * more desc */

    public TileEngineBase_BC8() {}

    public EnumActionResult attemptRotation() {
        EnumFacing[] possible = VanillaRotationHandlers.getAllSidesArray();
        EnumFacing current = currentDirection;
        int ord = VanillaRotationHandlers.getOrdinal(current, possible);
        for (int i = 1; i < possible.length; i++) {
            int next = (ord + i) % possible.length;
            EnumFacing toTry = possible[next];
            if (true) {// TODO: replace with sided check
                currentDirection = toTry;
                // makeTileCache();
                redrawBlock();
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.FAIL;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        currentDirection = NBTUtilBC.readEnum(nbt.getTag("currentDirection"), EnumFacing.class);
        if (currentDirection == null) {
            currentDirection = EnumFacing.UP;
        }
        heat = nbt.getDouble("heat");
        power = nbt.getLong("power");
        progress = nbt.getFloat("progress");
        progressPart = nbt.getInteger("progressPart");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setTag("currentDirection", NBTUtilBC.writeEnum(currentDirection));
        nbt.setDouble("heat", heat);
        nbt.setLong("power", power);
        nbt.setFloat("progress", progress);
        nbt.setInteger("progressPart", progressPart);
        return nbt;
    }

    @Override
    public void readPayload(int id, PacketBufferBC buffer, Side side, MessageContext ctx) throws IOException {
        super.readPayload(id, buffer, side, ctx);
        if (side == Side.CLIENT) {
            if (id == NET_RENDER_DATA) {
                isPumping = buffer.readBoolean();
                currentDirection = buffer.readEnumValue(EnumFacing.class);
                powerStage = buffer.readEnumValue(EnumPowerStage.class);
            } else if (id == NET_GUI_DATA) {
                heat = buffer.readFloat();
                currentOutput = buffer.readLong();
                power = buffer.readLong();

            } else if (id == NET_GUI_TICK) {
                heat = buffer.readFloat();
                currentOutput = buffer.readLong();
                power = buffer.readLong();

            }
        }
    }

    @Override
    public void writePayload(int id, PacketBufferBC buffer, Side side) {
        super.writePayload(id, buffer, side);
        if (side == Side.SERVER) {
            if (id == NET_RENDER_DATA) {
                buffer.writeBoolean(isPumping);
                buffer.writeEnumValue(currentDirection);
                buffer.writeEnumValue(powerStage);
            } else if (id == NET_GUI_DATA) {
                buffer.writeFloat((float) heat);
                buffer.writeLong(currentOutput);
                buffer.writeLong(power);

            } else if (id == NET_GUI_TICK) {
                buffer.writeFloat((float) heat);
                buffer.writeLong(currentOutput);
                buffer.writeLong(power);

            }
        }
    }

    public boolean onActivated(EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        return false;
    }

    protected Biome getBiome() {
        // TODO: Cache this!
        return world.getBiome(getPos());
    }

    /** @return The heat of the current biome, in celsius. */
    protected float getBiomeHeat() {
        Biome biome = getBiome();
        float temp = biome.getFloatTemperature(getPos());
        return Math.max(0, Math.min(30, temp * 15f));
    }

    public double getPowerLevel() {
        return power / (double) getMaxPower();
    }

    protected EnumPowerStage computePowerStage() {
        double heatLevel = getHeatLevel();
        if (heatLevel < 0.25f) return EnumPowerStage.BLUE;
        else if (heatLevel < 0.5f) return EnumPowerStage.GREEN;
        else if (heatLevel < 0.75f) return EnumPowerStage.YELLOW;
        else if (heatLevel < 0.85f) return EnumPowerStage.RED;
        else return EnumPowerStage.OVERHEAT;
    }

    public final EnumPowerStage getPowerStage() {
        if (!world.isRemote) {
            EnumPowerStage newStage = computePowerStage();

            if (powerStage != newStage) {
                powerStage = newStage;
                sendNetworkUpdate(NET_RENDER_DATA);
            }
        }

        return powerStage;
    }

    public void updateHeatLevel() {
        heat = ((MAX_HEAT - MIN_HEAT) * getPowerLevel()) + MIN_HEAT;
    }

    public double getHeatLevel() {
        return (heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
    }

    public double getIdealHeatLevel() {
        return heat / IDEAL_HEAT;
    }

    public double getHeat() {
        return heat;
    }

    public double getPistonSpeed() {
        if (!world.isRemote) {
            return Math.max(0.16 * getHeatLevel(), 0.01);
        }

        switch (getPowerStage()) {
            case BLUE:
                return 0.02;
            case GREEN:
                return 0.04;
            case YELLOW:
                return 0.08;
            case RED:
                return 0.16;
            default:
                return 0;
        }
    }

    @Nonnull
    protected abstract IMjConnector createConnector();

    @Override
    public void update() {
        deltaManager.tick();
        if (cannotUpdate()) return;

        boolean overheat = getPowerStage() == EnumPowerStage.OVERHEAT;

        if (world.isRemote) {
            lastProgress = progress;
            // idk if these will stay (at all) or in a more refined form
            double particleCount = 0;
            double flameRand = 0;
            if (powerStage == EnumPowerStage.BLUE) {
                particleCount = isPumping ? 0.125 : 0;
            } else if (powerStage == EnumPowerStage.GREEN) {
                particleCount = isPumping ? 0.25 : 0.125;
            } else if (powerStage == EnumPowerStage.YELLOW) {
                particleCount = isPumping ? 1 : 0.25;
                flameRand = 0.125;
            } else if (powerStage == EnumPowerStage.RED) {
                particleCount = isPumping ? 3 : 1.25;
                flameRand = 0.25;
            } else if (powerStage == EnumPowerStage.OVERHEAT) {
                particleCount = isPumping ? 5 : 4;
                flameRand = 0.5;
            }

            int realCount = (int) (Math.floor(particleCount)) + (world.rand.nextDouble() < (particleCount % 1) ? 1 : 0);

            if (realCount > 0) {
                double x = getPos().getX() + 0.5;
                double y = getPos().getY() + 0.5;
                double z = getPos().getZ() + 0.5;

                for (int i = realCount; i > 0; i--) {
                    double dx = (world.rand.nextDouble() - 0.5) * 0.5;
                    double dy = (world.rand.nextDouble() - 0.3) * 0.4;
                    double dz = (world.rand.nextDouble() - 0.5) * 0.5;

                    EnumParticleTypes type = world.rand.nextDouble() < flameRand ? EnumParticleTypes.FLAME : EnumParticleTypes.CLOUD;

                    world.spawnParticle(type, x, y, z, dx, dy, dz);
                }
                // world.playSound(x, y, z, SoundEvent, volume, pitch, distanceDelay);
            }
            return;
        }

        lastPower = 0;
        isRedstonePowered = world.isBlockIndirectlyGettingPowered(getPos()) > 0;

        if (!isRedstonePowered) {
            if (power > MjAPI.MJ) {
                power -= MjAPI.MJ;
            } else if (power > 0) {
                power = 0;
            }
        }

        updateHeatLevel();
        getPowerStage();
        engineUpdate();

        if (progressPart != 0) {
            progress += getPistonSpeed();

            if (progress > 0.5 && progressPart == 1) {
                progressPart = 2;
                sendPower(); // Comment out for constant power
            } else if (progress >= 1) {
                progress = 0;
                progressPart = 0;
            }
        } else if (isRedstonePowered && isActive()) {
            if (getPowerToExtract(false) > 0) {
                progressPart = 1;
                setPumping(true);
            } else {
                setPumping(false);
            }
        } else {
            setPumping(false);
        }

        // Uncomment for constant power
        // if (isRedstonePowered && isActive()) {
        // sendPower();
        // } else currentOutput = 0;

        if (!overheat) {
            burn();
        }
    }

    private long getPowerToExtract(boolean doExtract) {
        TileEntity tile = getTileBuffer(currentDirection).getTile();

        if (tile == null) return 0;

        if (tile.getClass() == getClass()) {
            TileEngineBase_BC8 other = (TileEngineBase_BC8) tile;
            return other.getMaxPower() - power;
        }

        IMjReceiver receiver = getReceiverToPower(tile, currentDirection);
        if (receiver == null) {
            return 0;
        }

        // Pulsed power
        return extractPower(0, receiver.getPowerRequested(), doExtract);
        // TODO: Use this:
        // return extractPower(receiver.getMinPowerReceived(), receiver.getMaxPowerReceived(), false);

        // Constant power
        // return extractEnergy(0, getActualOutput(), false); // Uncomment for constant power
    }

    private void sendPower() {
        TileEntity tile = getTileBuffer(currentDirection).getTile();
        if (tile == null) {
            return;
        }
        if (getClass() == tile.getClass()) {
            TileEngineBase_BC8 other = (TileEngineBase_BC8) tile;
            if (currentDirection == other.currentDirection) {
                other.power += extractPower(0, power, true);
            }
            return;
        }
        IMjReceiver receiver = getReceiverToPower(tile, currentDirection);
        if (receiver != null) {
            long extracted = getPowerToExtract(true);
            if (extracted > 0) {
                long excess = receiver.receivePower(extracted, false);
                extractPower(extracted - excess, extracted - excess, true); // Comment out for constant power
                // currentOutput = extractEnergy(0, needed, true); // Uncomment for constant power
            }
        }
    }

    // Uncomment out for constant power
    // public float getActualOutput() {
    // float heatLevel = getIdealHeatLevel();
    // return getCurrentOutput() * heatLevel;
    // }
    protected void burn() {}

    protected void engineUpdate() {
        if (!isRedstonePowered) {
            if (power >= 1) {
                power -= 1;
            } else if (power < 1) {
                power = 0;
            }
        }
    }

    public boolean isActive() {
        return true;
    }

    protected final void setPumping(boolean isActive) {
        if (this.isPumping == isActive) {
            return;
        }

        this.isPumping = isActive;
        sendNetworkUpdate(NET_RENDER_DATA);
    }

    // TEMP
    @FunctionalInterface
    public interface ITileBuffer {
        TileEntity getTile();
    }

    /** Temp! This should be replaced with a tile buffer! */
    public ITileBuffer getTileBuffer(EnumFacing side) {
        TileEntity tile = world.getTileEntity(getPos().offset(side));
        return () -> tile;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        // tileCache = null;
        // checkOrienation = true;
    }

    @Override
    public void validate() {
        super.validate();
        // tileCache = null;
        // checkOrienation = true;
    }

    /* STATE INFORMATION */
    public abstract boolean isBurning();

    // IPowerReceptor stuffs -- move!
    // @Override
    // public PowerReceiver getPowerReceiver(ForgeDirection side) {
    // return powerHandler.getPowerReceiver();
    // }
    //
    // @Override
    // public void doWork(PowerHandler workProvider) {
    // if (worldObj.isRemote) {
    // return;
    // }
    //
    // addEnergy(powerHandler.useEnergy(1, maxEnergyReceived(), true) * 0.95F);
    // }

    public void addPower(long microJoules) {
        power += microJoules;
        lastPower += microJoules;

        if (getPowerStage() == EnumPowerStage.OVERHEAT) {
            // TODO: turn engine off
            // worldObj.createExplosion(null, xCoord, yCoord, zCoord, explosionRange(), true);
            // worldObj.setBlockToAir(xCoord, yCoord, zCoord);
        }

        if (power > getMaxPower()) {
            power = getMaxPower();
        }
    }

    public long extractPower(long min, long max, boolean doExtract) {
        if (power < min) {
            return 0;
        }

        long actualMax;

        if (max > maxPowerExtracted()) {
            actualMax = maxPowerExtracted();
        } else {
            actualMax = max;
        }

        if (actualMax < min) {
            return 0;
        }

        long extracted;

        if (power >= actualMax) {
            extracted = actualMax;

            if (doExtract) {
                power -= actualMax;
            }
        } else {
            extracted = power;

            if (doExtract) {
                power = 0;
            }
        }

        return extracted;
    }

    public final boolean isPoweredTile(TileEntity tile, EnumFacing side) {
        if (tile == null) return false;
        if (tile.getClass() == getClass()) {
            TileEngineBase_BC8 other = (TileEngineBase_BC8) tile;
            return other.currentDirection == currentDirection;
        }
        return getReceiverToPower(tile, side) != null;
    }

    /** Redstone engines override this to get an {@link IMjRedstoneReceiver} instance */
    public IMjReceiver getReceiverToPower(TileEntity tile, EnumFacing side) {
        if (tile == null) return null;
        IMjReceiver rec = tile.getCapability(MjAPI.CAP_RECEIVER, side.getOpposite());
        if (rec != null && rec.canConnect(mjConnector)) {
            return rec;
        } else {
            return null;
        }
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapUtil.CAP_ITEMS && facing == currentDirection) {
            return null;
        }
        T cap = mjCaps.getCapability(capability, facing);
        if (cap != null) {
            return cap;
        }
        return super.getCapability(capability, facing);
    }

    public abstract long getMaxPower();

    public long minPowerReceived() {
        return 2 * MjAPI.MJ;
    }

    public abstract long maxPowerReceived();

    public abstract long maxPowerExtracted();

    public abstract float explosionRange();

    public long getEnergyStored() {
        return power;
    }

    public abstract long getCurrentOutput();

    public boolean isEngineOn() {
        return isPumping;
    }

    @SideOnly(Side.CLIENT)
    public float getProgressClient(float partialTicks) {
        return lastProgress * (1 - partialTicks) + progress * partialTicks;
    }

    public EnumFacing getCurrentFacing() {
        return currentDirection;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getDebugInfo(List<String> left, List<String> right, EnumFacing side) {
        left.add("");
        left.add("facing = " + currentDirection);
        left.add("heat = " + LocaleUtil.localizeHeat(heat) + " -- " + String.format("%.2f %%", getHeatLevel()));
        left.add("power = " + MjAPI.formatMjShort(power));
        left.add("stage = " + powerStage);
        left.add("progress = " + progress);
        left.add("last = +" + MjAPI.formatMjShort(lastPower));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasFastRenderer() {
        return true;
    }
}
