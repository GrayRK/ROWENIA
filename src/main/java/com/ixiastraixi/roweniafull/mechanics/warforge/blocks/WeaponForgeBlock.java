package com.ixiastraixi.roweniafull.mechanics.warforge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import com.ixiastraixi.roweniafull.mechanics.warforge.blockentity.WeaponForgeBlockEntity;
import com.ixiastraixi.roweniafull.registry.warforge.WarforgeBlockEntities;
import net.minecraft.world.Containers;



public class WeaponForgeBlock extends BaseEntityBlock implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<WeaponForgeHalf> HALF =
            EnumProperty.create("half", WeaponForgeHalf.class);

    private static final VoxelShape LEFT_SHAPE  = Shapes.block();
    private static final VoxelShape RIGHT_SHAPE = Shapes.block();

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public WeaponForgeBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .noOcclusion()
        );
        this.registerDefaultState(
                this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, WeaponForgeHalf.LEFT)
                .setValue(LIT, Boolean.FALSE)
                );
    }

    enum WeaponForgeHalf implements net.minecraft.util.StringRepresentable {
        LEFT, RIGHT;
        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }

    /* ============== размещение ============== */

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite(); // «смотрим» на игрока
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        BlockPos rightPos = pos.relative(facing.getClockWise());
        if (!level.getBlockState(rightPos).canBeReplaced(ctx)) return null;
        return defaultBlockState().setValue(FACING, facing).setValue(HALF, WeaponForgeHalf.LEFT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        BlockPos rightPos = pos.relative(facing.getClockWise());
        level.setBlock(rightPos, state.setValue(HALF, WeaponForgeHalf.RIGHT), UPDATE_CLIENTS);
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    /* ============== снятие/поддержка пары ============== */

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            // 1) Дропнуть инвентарь всегда с ЛЕВОЙ половины (где BE), независимо от того, что ломали
            if (!level.isClientSide) {
                Direction facing = state.getValue(FACING);
                BlockPos leftPos = (state.getValue(HALF) == WeaponForgeHalf.LEFT)
                        ? pos
                        : pos.relative(facing.getCounterClockWise());

                BlockEntity be = level.getBlockEntity(leftPos);
                if (be instanceof WeaponForgeBlockEntity forge) {
                    // высыпаем содержимое и очищаем, чтобы не задублилось,
                    // когда мы сейчас удалим вторую половину
                    Containers.dropContents(level, leftPos, forge.container());
                    forge.container().clearContent();
                    forge.dropFuelBuffer();
                }
            }

            // 2) Снести вторую половину пары, если она ещё стоит
            BlockPos other = otherHalfPos(state, pos);
            BlockState otherState = level.getBlockState(other);
            if (otherState.getBlock() == this) {
                level.removeBlock(other, false);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction fromDir, BlockState fromState,
                                  LevelAccessor level, BlockPos pos, BlockPos fromPos) {
        if (fromPos.equals(otherHalfPos(state, pos)) && fromState.getBlock() != this) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, fromDir, fromState, level, pos, fromPos);
    }

    private static BlockPos otherHalfPos(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.getValue(HALF) == WeaponForgeHalf.LEFT
                ? pos.relative(facing.getClockWise())
                : pos.relative(facing.getCounterClockWise());
    }

    /* ============== взаимодействие / GUI ============== */

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos leftPos = (state.getValue(HALF) == WeaponForgeHalf.LEFT)
                ? pos
                : pos.relative(state.getValue(FACING).getCounterClockWise());

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(leftPos);
            if (be instanceof MenuProvider provider && player instanceof ServerPlayer sp) {
                NetworkHooks.openScreen(sp, provider, leftPos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /* ============== поворот / отражение ============== */

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    /* ============== BlockEntity ============== */

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // только левая половина содержит BE
        if (state.getValue(HALF) == WeaponForgeHalf.LEFT) {
            return new WeaponForgeBlockEntity(pos, state);
        }
        return null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (state.getValue(HALF) != WeaponForgeHalf.LEFT) return null;

        // безопасный тикер без ручных кастов (helper доступен из BaseEntityBlock)
        return createTickerHelper(
                type,
                WarforgeBlockEntities.WEAPON_FORGE_BE.get(),
                WeaponForgeBlockEntity::serverTick // static (Level, BlockPos, BlockState, WeaponForgeBlockEntity)
        );
    }

    /* ============== визуал/форма/состояния ============== */

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(HALF) == WeaponForgeHalf.LEFT ? LEFT_SHAPE : RIGHT_SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(LIT)) return;

        // если дым только у "левой" половины с BE — оставь проверку
        if (state.getValue(HALF) != WeaponForgeHalf.LEFT) return;

        Direction f = state.getValue(FACING);
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D; // высота дымка над печкой
        double z = pos.getZ() + 0.5D;

        // небольшой вынос вперёд от лицевой стороны
        double off = 0.6D;
        switch (f) {
            case NORTH -> z -= off;
            case SOUTH -> z += off;
            case WEST  -> x -= off;
            case EAST  -> x += off;
        }

        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.02D, 0.0D);
        if (rand.nextDouble() < 0.2) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y - 0.06D, z, 0.0D, 0.0D, 0.0D);
        }

        // звук плавильни — как у ванили, с редкой трескотнёй
        if (rand.nextDouble() < 0.1D) {
            level.playLocalSound(x, y, z,
                    SoundEvents.FURNACE_FIRE_CRACKLE, // ванильный "треск печки"
                    SoundSource.BLOCKS,
                    0.4F, 1.0F, false);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        b.add(FACING, HALF, LIT);
    }
}
