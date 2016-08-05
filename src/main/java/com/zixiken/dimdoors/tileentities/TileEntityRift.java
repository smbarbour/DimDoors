package com.zixiken.dimdoors.tileentities;

import java.util.List;
import java.util.Random;

import com.zixiken.dimdoors.DimDoors;
import com.zixiken.dimdoors.config.DDProperties;
import com.zixiken.dimdoors.core.DimLink;
import com.zixiken.dimdoors.core.PocketManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.AxisAlignedBB;
import com.zixiken.dimdoors.core.NewDimData;
import com.zixiken.dimdoors.util.Point4D;
import com.zixiken.dimdoors.watcher.ClientLinkData;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ITickable;

public class TileEntityRift extends DDTileEntityBase implements ITickable
{
	private static final int RIFT_INTERACTION_RANGE = 5;
	private static final int MAX_ANCESTOR_LINKS = 2;
	private static final int MAX_CHILD_LINKS = 1;
	private static final int ENDERMAN_SPAWNING_CHANCE = 1;
	private static final int MAX_ENDERMAN_SPAWNING_CHANCE = 32;
	private static final int RIFT_SPREAD_CHANCE = 1;
	private static final int MAX_RIFT_SPREAD_CHANCE = 256;
	private static final int HOSTILE_ENDERMAN_CHANCE = 1;
	private static final int MAX_HOSTILE_ENDERMAN_CHANCE = 3;
	private static final int UPDATE_PERIOD = 200;
	private static final int CLOSING_PERIOD = 40;

	private static Random random = new Random();

	private int updateTimer;
	private int closeTimer = 0;
	public BlockPos offset = BlockPos.ORIGIN;
	public boolean shouldClose = false;
	public Point4D nearestRiftLocation = null;
	public int spawnedEndermenID = 0;
	
	public int riftRotation = random.nextInt(360);
	public float growth = 0;
	
	public TileEntityRift() {
		// Vary the update times of rifts to prevent all the rifts in a cluster
		// from updating at the same time.
		updateTimer = random.nextInt(UPDATE_PERIOD);
	}
	
	@Override
	public void update() {
		if (PocketManager.getLink(pos, worldObj.provider.getDimensionId()) == null) {
			if (worldObj.getBlockState(pos).getBlock() == DimDoors.blockRift) {
				worldObj.setBlockToAir(pos);
			} else {
				invalidate();
			}
			return;
		}
		
		if (worldObj.getBlockState(pos).getBlock() != DimDoors.blockRift) {
			invalidate();
			return;
		}
		

		// Check if this rift should render white closing particles and
		// spread the closing effect to other rifts nearby.
		if (shouldClose) {
			closeRift();
			return;
		}
		
		if (updateTimer >= UPDATE_PERIOD) {
			spawnEndermen(DimDoors.properties);
			updateTimer = 0;
		}
		else if (updateTimer == UPDATE_PERIOD / 2) {
			updateNearestRift();
			spread(DimDoors.properties);
		}
		growth += 1F/(growth+1);
		updateTimer++;
	}

	private void spawnEndermen(DDProperties properties)
	{
		if (worldObj.isRemote || !properties.RiftsSpawnEndermenEnabled) {
			return;
		}

		// Ensure that this rift is only spawning one Enderman at a time, to prevent hordes of Endermen
		Entity entity = worldObj.getEntityByID(this.spawnedEndermenID);
		if (entity != null && entity instanceof EntityEnderman) {
			return;
		}

		if (random.nextInt(MAX_ENDERMAN_SPAWNING_CHANCE) < ENDERMAN_SPAWNING_CHANCE) {
			// Endermen will only spawn from groups of rifts
			if (updateNearestRift()) {
				List<EntityEnderman> list =  worldObj.getEntitiesWithinAABB(EntityEnderman.class,
						AxisAlignedBB.fromBounds(pos.getX() - 9, pos.getY() - 3, pos.getZ() - 9, pos.getX() + 9, pos.getY() + 3, pos.getZ() + 9));

				if (list.isEmpty()) {
					EntityEnderman enderman = new EntityEnderman(worldObj);
					enderman.setLocationAndAngles(pos.getX() + 0.5, pos.getY() - 1, pos.getZ() + 0.5, 5, 6);
					worldObj.spawnEntityInWorld(enderman);
					
					if (random.nextInt(MAX_HOSTILE_ENDERMAN_CHANCE) < HOSTILE_ENDERMAN_CHANCE) {
						EntityPlayer player = this.worldObj.getClosestPlayerToEntity(enderman, 50);
						if (player != null) {
							enderman.setAttackTarget(player);
						}
					}
				}				
			}
		}
	}

	private void closeRift() {
		NewDimData dimension = PocketManager.createDimensionData(worldObj);
		if (growth < CLOSING_PERIOD / 2) {
			for (DimLink riftLink : dimension.findRiftsInRange(worldObj, 6, pos)) {
				Point4D location = riftLink.source();
				TileEntityRift rift = (TileEntityRift) worldObj.getTileEntity(location.toBlockPos());
				if (rift != null && !rift.shouldClose) {
					rift.shouldClose = true;
					rift.markDirty();
				}
			}
		}
		if (growth <= 0 && !worldObj.isRemote) {
			DimLink link = PocketManager.getLink(pos, worldObj);
			if (link != null && !worldObj.isRemote) {
				dimension.deleteLink(link);
			}

			worldObj.setBlockToAir(pos);
			worldObj.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, "mods.DimDoors.sfx.riftClose", 0.7f, 1, false);
		}
	
		growth --;
	}
	
	public boolean updateNearestRift() {
		Point4D previousNearest = nearestRiftLocation;
		DimLink nearestRiftLink = PocketManager.createDimensionData(worldObj).findNearestRift(
				worldObj, RIFT_INTERACTION_RANGE, pos);
		
		nearestRiftLocation = (nearestRiftLink == null) ? null : nearestRiftLink.source();

		// If the nearest rift location changed, then update particle offsets
		if (previousNearest != nearestRiftLocation &&
			(previousNearest == null || nearestRiftLocation == null || !previousNearest.equals(nearestRiftLocation)))
		{
			updateParticleOffsets();
		}
		return (nearestRiftLocation != null);
	}

	private void updateParticleOffsets() {
		if (nearestRiftLocation != null) {
			this.offset = this.pos.subtract(nearestRiftLocation.toBlockPos());
		} else {
			this.offset = BlockPos.ORIGIN;
		}
		this.markDirty();
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}

	public int countAncestorLinks(DimLink link) {
		if (link.parent() != null) {
			return countAncestorLinks(link.parent()) + 1;
		}

		return 0;
	}

	public void spread(DDProperties properties) {
		if (worldObj.isRemote || !properties.RiftSpreadEnabled
			|| random.nextInt(MAX_RIFT_SPREAD_CHANCE) < RIFT_SPREAD_CHANCE || this.shouldClose) {
			return;
		}

		NewDimData dimension = PocketManager.createDimensionData(worldObj);
		DimLink link = dimension.getLink(pos);
		
		if (link.childCount() >= MAX_CHILD_LINKS || countAncestorLinks(link) >= MAX_ANCESTOR_LINKS) {
			return;
		}
		
		// The probability of rifts trying to spread increases if more rifts are nearby.
		// Players should see rifts spread faster within clusters than at the edges of clusters.
		// Also, single rifts CANNOT spread.
		int nearRifts = dimension.findRiftsInRange(worldObj, RIFT_INTERACTION_RANGE, pos).size();
		if (nearRifts == 0 || random.nextInt(nearRifts) == 0) {
			return;
		}

		DimDoors.blockRift.spreadRift(dimension, link, worldObj, random);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.updateTimer = nbt.getInteger("updateTimer");
		this.offset = new BlockPos(nbt.getInteger("xOffset"), nbt.getInteger("yOffset"), nbt.getInteger("zOffset"));
		this.shouldClose = nbt.getBoolean("shouldClose");
		this.spawnedEndermenID = nbt.getInteger("spawnedEndermenID");
		this.riftRotation = nbt.getInteger("riftRotation");
		this.growth = nbt.getFloat("growth");

	}

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		nbt.setInteger("updateTimer", this.updateTimer);
		nbt.setInteger("xOffset", this.offset.getX());
		nbt.setInteger("yOffset", this.offset.getY());
		nbt.setInteger("zOffset", this.offset.getZ());
		nbt.setBoolean("shouldClose", this.shouldClose);
		nbt.setInteger("spawnedEndermenID", this.spawnedEndermenID);
		nbt.setInteger("riftRotation", this.riftRotation);
		nbt.setFloat("growth", this.growth);

	}

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);

        if(PocketManager.getLink(pos, worldObj)!=null) {
            ClientLinkData linkData = new ClientLinkData(PocketManager.getLink(pos, worldObj));

            NBTTagCompound link = new NBTTagCompound();
            linkData.writeToNBT(link);

            tag.setTag("Link", link);
        }
        return new S35PacketUpdateTileEntity(this.pos, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        NBTTagCompound tag = pkt.getNbtCompound();
        readFromNBT(tag);

        if (tag.hasKey("Link")) {
            ClientLinkData linkData = ClientLinkData.readFromNBT(tag.getCompoundTag("Link"));
            PocketManager.getLinkWatcher().onCreated(linkData);
        }
    }

	@Override
	public float[] getRenderColor(Random rand)
	{
		return null;
	}
}