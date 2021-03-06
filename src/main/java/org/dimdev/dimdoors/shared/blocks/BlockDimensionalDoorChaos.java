package org.dimdev.dimdoors.shared.blocks;

import java.util.Random;

import net.minecraft.init.Blocks;
import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.shared.items.ModItems;
import org.dimdev.dimdoors.shared.tileentities.TileEntityEntranceRift;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class BlockDimensionalDoorChaos extends BlockDimensionalDoor { // TODO: different entrance color

    public static final String ID = "chaos_dimensional_door";

    public BlockDimensionalDoorChaos() {
        super(Material.IRON);
        setHardness(0.2F);
        setUnlocalizedName(ID);
        setRegistryName(new ResourceLocation(DimDoors.MODID, ID));
        setLightLevel(0.0F);
    }

    @Override
    public Item getItem() {
        return ModItems.CHAOS_DOOR;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Blocks.IRON_DOOR.getItemDropped(state, rand, fortune);
    }

    @Override
    public void setupRift(TileEntityEntranceRift rift) {
        // TODO
    }

    @Override
    public boolean canBePlacedOnRift() {
        return false;
    }
}
