package org.dimdev.dimdoors.shared.items;

import net.minecraft.util.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.shared.RayTraceHelper;
import org.dimdev.dimdoors.shared.sound.ModSounds;
import org.dimdev.dimdoors.shared.tileentities.TileEntityFloatingRift;
import org.dimdev.ddutils.I18nUtils;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.util.List;

public class ItemRiftRemover extends Item {
    public static final String ID = "rift_remover";

    public ItemRiftRemover() {
        setCreativeTab(DimDoors.DIM_DOORS_CREATIVE_TAB);
        setUnlocalizedName(ID);
        setRegistryName(new ResourceLocation(DimDoors.MODID, ID));
        setMaxStackSize(1);
        setMaxDamage(100);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.addAll(I18nUtils.translateMultiline("info.rift_remover"));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand handIn) { // TODO: permissions
        ItemStack stack = player.getHeldItem(handIn);

        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        RayTraceResult hit = rayTrace(world, player, true);
        if (RayTraceHelper.isFloatingRift(hit, world)) {
            TileEntityFloatingRift rift = (TileEntityFloatingRift) world.getTileEntity(hit.getBlockPos());
            world.setBlockState(rift.getPos(), Blocks.AIR.getDefaultState());
            world.playSound(null, player.getPosition(), ModSounds.RIFT_CLOSE, SoundCategory.BLOCKS, 0.6f, 1);
            // TODO: render rift removing animation

            stack.damageItem(10, player);
            DimDoors.sendMessage(player, "Rift Removed");
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.FAIL, stack);
    }
}
