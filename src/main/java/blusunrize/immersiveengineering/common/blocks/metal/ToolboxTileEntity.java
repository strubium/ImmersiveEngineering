/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks.metal;

import blusunrize.immersiveengineering.api.IEApi;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.*;
import blusunrize.immersiveengineering.common.items.IEItems.Tools;
import blusunrize.immersiveengineering.common.items.InternalStorageItem;
import blusunrize.immersiveengineering.common.items.ToolboxItem;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.google.common.collect.ImmutableList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.storage.loot.LootContext.Builder;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.List;

public class ToolboxTileEntity extends IEBaseTileEntity implements IDirectionalTile, IBlockBounds, IIEInventory, IInteractionObjectIE, ITileDrop, IPlayerInteraction
{
	public static TileEntityType<ToolboxTileEntity> TYPE;

	NonNullList<ItemStack> inventory = NonNullList.withSize(ToolboxItem.SLOT_COUNT, ItemStack.EMPTY);
	public ITextComponent name;
	private Direction facing = Direction.NORTH;
	private ListNBT enchantments;

	public ToolboxTileEntity()
	{
		super(TYPE);
	}

	@Override
	public void readCustomNBT(CompoundNBT nbt, boolean descPacket)
	{
		facing = Direction.byIndex(nbt.getInt("facing"));
		if(nbt.contains("name", NBT.TAG_STRING))
			this.name = ITextComponent.Serializer.fromJson(nbt.getString("name"));
		if(nbt.contains("enchantments", NBT.TAG_LIST))
			this.enchantments = nbt.getList("enchantments", NBT.TAG_COMPOUND);
		if(!descPacket)
			inventory = Utils.readInventory(nbt.getList("inventory", NBT.TAG_COMPOUND), ToolboxItem.SLOT_COUNT);
	}

	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket)
	{
		nbt.putInt("facing", facing.ordinal());
		if(this.name!=null)
			nbt.putString("name", ITextComponent.Serializer.toJson(this.name));
		if(this.enchantments!=null)
			nbt.put("enchantments", this.enchantments);
		if(!descPacket)
			nbt.put("inventory", Utils.writeInventory(inventory));
	}

	@Override
	public boolean interact(Direction side, PlayerEntity player, Hand hand, ItemStack heldItem, float hitX, float hitY, float hitZ)
	{
		if(player.isSneaking())
		{
			if(!world.isRemote)
			{
				ItemEntity entityitem = new ItemEntity(world, getPos().getX()+.5, getPos().getY()+.5,
						getPos().getZ()+.5, getPickBlock(player, getBlockState(), new BlockRayTraceResult(new Vec3d(hitX, hitY, hitZ), side, pos, false)));
				entityitem.setDefaultPickupDelay();
				world.removeBlock(getPos(), false);
				world.addEntity(entityitem);
			}
			return true;
		}
		return false;
	}

	//TODO
	//@Override
	//@Nullable
	//public ITextComponent getDisplayName()
	//{
	//	return name!=null?new TextComponentString(name): new TextComponentTranslation("item.immersiveengineering.toolbox.name");
	//}

	@Override
	public boolean canUseGui(PlayerEntity player)
	{
		return true;
	}

	@Override
	public ResourceLocation getGuiName()
	{
		return Lib.GUIID_ToolboxBlock;
	}

	@Override
	public IInteractionObjectIE getGuiMaster()
	{
		return this;
	}

	@Override
	public NonNullList<ItemStack> getInventory()
	{
		return inventory;
	}

	@Override
	public boolean isStackValid(int slot, ItemStack stack)
	{
		return IEApi.isAllowedInCrate(stack);
	}

	@Override
	public int getSlotLimit(int slot)
	{
		return 64;
	}

	@Override
	public void doGraphicalUpdates(int slot)
	{
		this.markDirty();
	}

	@Override
	public List<ItemStack> getTileDrops(Builder context)
	{
		ItemStack stack = new ItemStack(Tools.toolbox);
		((InternalStorageItem)Tools.toolbox).setContainedItems(stack, inventory);
		if(this.name!=null)
			stack.setDisplayName(this.name);
		if(enchantments!=null)
			stack.getOrCreateTag().put("ench", enchantments);
		return ImmutableList.of(stack);
	}

	@Override
	public void readOnPlacement(LivingEntity placer, ItemStack stack)
	{
		if(stack.getItem() instanceof InternalStorageItem)
		{
			stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(inv ->
			{
				inventory = NonNullList.withSize(inv.getSlots(), ItemStack.EMPTY);
				for(int i = 0; i < inv.getSlots(); i++)
					inventory.set(i, inv.getStackInSlot(i));
			});

			if(stack.hasDisplayName())
				this.name = stack.getDisplayName();
			enchantments = stack.getEnchantmentTagList();
		}
	}

	@Override
	public boolean preventInventoryDrop()
	{
		return true;
	}

	@Override
	public Direction getFacing()
	{
		return facing;
	}

	@Override
	public void setFacing(Direction facing)
	{
		this.facing = facing;
	}

	@Override
	public int getFacingLimitation()
	{
		return 2;
	}

	@Override
	public boolean mirrorFacingOnPlacement(LivingEntity placer)
	{
		return false;
	}

	@Override
	public boolean canHammerRotate(Direction side, float hitX, float hitY, float hitZ, LivingEntity entity)
	{
		return false;
	}

	@Override
	public boolean canRotate(Direction axis)
	{
		return true;
	}

	private static final float[] boundsZ = {.125f, 0, .25f, .875f, .625f, .75f};
	private static final float[] boundsX = {.25f, 0, .125f, .75f, .625f, .875f};

	@Override
	public float[] getBlockBounds()
	{
		return facing.getAxis()==Axis.Z?boundsZ: boundsX;
	}
}