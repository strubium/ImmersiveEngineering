/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.blocks;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.common.IEContent;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.property.IUnlistedProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class BlockIEBase extends Block
{
	protected static IProperty[] tempProperties;
	protected static IUnlistedProperty[] tempUnlistedProperties;

	public final String name;
	public final IProperty[] additionalProperties;
	public final IUnlistedProperty[] additionalUnlistedProperties;
	boolean isHidden;
	boolean hasFlavour;
	protected Set<BlockRenderLayer> renderLayers = Sets.newHashSet(BlockRenderLayer.SOLID);
	//TODO wtf is variable opacity?
	protected int lightOpacity;
	protected EnumPushReaction mobilityFlag = EnumPushReaction.NORMAL;
	protected boolean canHammerHarvest;
	protected boolean notNormalBlock;
	private boolean opaqueCube = false;

	public BlockIEBase(String name, Block.Properties blockProps, Class<? extends ItemBlockIEBase> itemBlock, IProperty... additionalProperties)
	{
		super(setTempProperties(blockProps, additionalProperties));
		this.name = name;

		this.additionalProperties = Arrays.copyOf(tempProperties, tempProperties.length);
		this.additionalUnlistedProperties = Arrays.copyOf(tempUnlistedProperties, tempUnlistedProperties.length);
		this.setDefaultState(getInitDefaultState());
		String registryName = createRegistryName();
		//TODO this.setCreativeTab(ImmersiveEngineering.itemGroup);
		//TODO this.adjustSound();

		IEContent.registeredIEBlocks.add(this);
		try
		{
			Item.Properties itemProps = new Item.Properties().group(ImmersiveEngineering.itemGroup);
			IEContent.registeredIEItems.add(itemBlock.getConstructor(Block.class, Item.Properties.class)
					.newInstance(this, itemProps));
		} catch(Exception e)
		{
			//TODO e.printStackTrace();
			throw new RuntimeException(e);
		}
		lightOpacity = 255;
	}

	//TODO do we still need this hackyness?
	protected static Block.Properties setTempProperties(Properties blockProps, Object[] additionalProperties)
	{
		ArrayList<IProperty> propList = new ArrayList<IProperty>();
		ArrayList<IUnlistedProperty> unlistedPropList = new ArrayList<IUnlistedProperty>();
		for(Object o : additionalProperties)
		{
			if(o instanceof IProperty)
				propList.add((IProperty)o);
			if(o instanceof IProperty[])
				propList.addAll(Arrays.asList(((IProperty[])o)));
			if(o instanceof IUnlistedProperty)
				unlistedPropList.add((IUnlistedProperty)o);
			if(o instanceof IUnlistedProperty[])
				unlistedPropList.addAll(Arrays.asList(((IUnlistedProperty[])o)));
		}
		tempProperties = propList.toArray(new IProperty[0]);
		tempUnlistedProperties = unlistedPropList.toArray(new IUnlistedProperty[0]);
		return blockProps;
	}

	protected static Object[] combineProperties(Object[] currentProperties, Object... addedProperties)
	{
		Object[] array = new Object[currentProperties.length+addedProperties.length];
		for(int i = 0; i < currentProperties.length; i++)
			array[i] = currentProperties[i];
		for(int i = 0; i < addedProperties.length; i++)
			array[currentProperties.length+i] = addedProperties[i];
		return array;
	}

	public BlockIEBase setHidden(boolean shouldHide)
	{
		isHidden = shouldHide;
		return this;
	}

	public boolean isHidden()
	{
		return isHidden;
	}

	public BlockIEBase setHasFlavour(boolean shouldHave)
	{
		hasFlavour = shouldHave;
		return this;
	}

	public boolean hasFlavour()
	{
		return hasFlavour;
	}

	public BlockIEBase setBlockLayer(BlockRenderLayer... layer)
	{
		this.renderLayers = Sets.newHashSet(layer);
		return this;
	}

	@Override
	public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer)
	{
		return renderLayers.contains(layer);
	}

	public BlockIEBase setLightOpacity(int opacity)
	{
		lightOpacity = opacity;
		return this;
	}

	@Override
	public int getLightValue(IBlockState state, IWorldReader world, BlockPos pos)
	{
		return lightOpacity;
	}

	public BlockIEBase setMobility(EnumPushReaction flag)
	{
		mobilityFlag = flag;
		return this;
	}

	@Override
	public EnumPushReaction getPushReaction(IBlockState state)
	{
		return mobilityFlag;
	}

	public BlockIEBase setNotNormalBlock()
	{
		notNormalBlock = true;
		return this;
	}

	@Override
	public boolean isFullCube(IBlockState state)
	{
		return notNormalBlock;
	}


/*TODO does this still exist?	@Override
	public boolean isOpaqueCube(IBlockState state)
	{
		return notNormalBlock;
	}*/

	@Override
	public boolean causesSuffocation(IBlockState state)
	{
		return !notNormalBlock;
	}

	@Override
	public boolean isNormalCube(IBlockState state, IBlockReader world, BlockPos pos)
	{
		return notNormalBlock;
	}

	@Override
	protected void fillStateContainer(Builder<Block, IBlockState> builder)
	{
		super.fillStateContainer(builder);
		//TODO ext states?
		builder.add(tempProperties);
	}

	protected IBlockState getInitDefaultState()
	{
		return this.stateContainer.getBaseState();
	}

	protected <V extends Comparable<V>> IBlockState applyProperty(IBlockState in, IProperty<V> prop, Object val)
	{
		return in.with(prop, (V)val);
	}

	public void onIEBlockPlacedBy(World world, BlockPos pos, IBlockState state, EnumFacing side, float hitX, float hitY, float hitZ, EntityLivingBase placer, ItemStack stack)
	{
	}

	public boolean canIEBlockBePlaced(World world, BlockPos pos, IBlockState newState, EnumFacing side, float hitX, float hitY, float hitZ, EntityPlayer player, ItemStack stack)
	{
		return true;
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
	{
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items)
	{
		items.add(new ItemStack(this, 1));
	}

	@Override
	public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int eventID, int eventParam)
	{
		if(worldIn.isRemote&&eventID==255)
		{
			worldIn.notifyBlockUpdate(pos, state, state, 3);
			return true;
		}
		return super.eventReceived(state, worldIn, pos, eventID, eventParam);
	}

	public BlockIEBase setHammerHarvest()
	{
		canHammerHarvest = true;
		return this;
	}

	public boolean allowHammerHarvest(IBlockState blockState)
	{
		return canHammerHarvest;
	}

	public boolean allowWirecutterHarvest(IBlockState blockState)
	{
		return false;
	}

	public boolean isOpaqueCube()
	{
		return opaqueCube;
	}

	public BlockIEBase setOpaque(boolean isOpaque)
	{
		opaqueCube = isOpaque;
		return this;
	}

	@Override
	public boolean isToolEffective(IBlockState state, ToolType tool)
	{
		if(allowHammerHarvest(state)&&tool==IEContent.toolHammer)
			return true;
		if(allowWirecutterHarvest(state)&&IEContent.toolWireCutter)
			return true;
		return super.isToolEffective(state, tool);
	}

	public String createRegistryName()
	{
		return ImmersiveEngineering.MODID+":"+name;
	}

	public abstract static class IELadderBlock extends BlockIEBase
	{
		public IELadderBlock(String name, Block.Properties material,
							 Class<? extends ItemBlockIEBase> itemBlock, IProperty... additionalProperties)
		{
			super(name, material, itemBlock, additionalProperties);
		}

		@Override
		public void onEntityCollision(IBlockState state, World worldIn, BlockPos pos, Entity entityIn)
		{
			super.onEntityCollision(state, worldIn, pos, entityIn);
			if(entityIn instanceof EntityLivingBase&&!((EntityLivingBase)entityIn).isOnLadder()&&isLadder(state, worldIn, pos, (EntityLivingBase)entityIn))
			{
				float f5 = 0.15F;
				if(entityIn.motionX < -f5)
					entityIn.motionX = -f5;
				if(entityIn.motionX > f5)
					entityIn.motionX = f5;
				if(entityIn.motionZ < -f5)
					entityIn.motionZ = -f5;
				if(entityIn.motionZ > f5)
					entityIn.motionZ = f5;

				entityIn.fallDistance = 0.0F;
				if(entityIn.motionY < -0.15D)
					entityIn.motionY = -0.15D;

				if(entityIn.motionY < 0&&entityIn instanceof EntityPlayer&&entityIn.isSneaking())
				{
					entityIn.motionY = 0;
					return;
				}
				if(entityIn.collidedHorizontally)
					entityIn.motionY = .2;
			}
		}
	}
}