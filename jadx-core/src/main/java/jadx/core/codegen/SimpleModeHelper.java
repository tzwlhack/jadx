package jadx.core.codegen;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.TargetInsnNode;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.visitors.blocks.BlockProcessor;
import jadx.core.dex.visitors.blocks.BlockSplitter;
import jadx.core.utils.BlockUtils;

public class SimpleModeHelper {

	private final MethodNode mth;

	private final BitSet startLabel;
	private final BitSet endGoto;

	public SimpleModeHelper(MethodNode mth) {
		this.mth = mth;
		this.startLabel = BlockUtils.newBlocksBitSet(mth);
		this.endGoto = BlockUtils.newBlocksBitSet(mth);
	}

	public List<BlockNode> prepareBlocks() {
		removeEmptyBlocks();
		List<BlockNode> blocksList = getSortedBlocks();
		blocksList.removeIf(b -> b.equals(mth.getEnterBlock()) || b.equals(mth.getExitBlock()));
		unbindExceptionHandlers();
		@Nullable
		BlockNode prev = null;
		int blocksCount = blocksList.size();
		for (int i = 0; i < blocksCount; i++) {
			BlockNode block = blocksList.get(i);
			List<BlockNode> preds = block.getPredecessors();
			int predsCount = preds.size();
			if (predsCount > 1) {
				startLabel.set(block.getId());
			} else if (predsCount == 1 && prev != null) {
				if (!prev.equals(preds.get(0))) {
					startLabel.set(block.getId());
					if (prev.getSuccessors().size() == 1 && !prev.contains(AFlag.RETURN)) {
						endGoto.set(prev.getId());
					}
				}
			}
			InsnNode lastInsn = BlockUtils.getLastInsn(block);
			if (lastInsn instanceof TargetInsnNode) {
				processTargetInsn(block, lastInsn);
			}
			if (block.contains(AType.EXC_HANDLER)) {
				startLabel.set(block.getId());
			}
			if (i + 1 >= blocksCount && !mth.isPreExitBlocks(block)) {
				endGoto.set(block.getId());
			}
			prev = block;
		}
		return blocksList;
	}

	private void removeEmptyBlocks() {
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block.getInstructions().isEmpty()
					&& block.getPredecessors().size() > 0
					&& block.getSuccessors().size() == 1) {
				BlockNode successor = block.getSuccessors().get(0);
				List<BlockNode> predecessors = block.getPredecessors();
				BlockSplitter.removeConnection(block, successor);
				if (predecessors.size() == 1) {
					BlockSplitter.replaceConnection(predecessors.get(0), block, successor);
				} else {
					for (BlockNode pred : new ArrayList<>(predecessors)) {
						BlockSplitter.replaceConnection(pred, block, successor);
					}
				}
				block.add(AFlag.REMOVE);
			}
		}
		BlockProcessor.removeMarkedBlocks(mth);
	}

	private void unbindExceptionHandlers() {
		if (mth.isNoExceptionHandlers()) {
			return;
		}
		for (ExceptionHandler handler : mth.getExceptionHandlers()) {
			BlockNode handlerBlock = handler.getHandlerBlock();
			if (handlerBlock != null) {
				BlockSplitter.removePredecessors(handlerBlock);
			}
		}
	}

	private void processTargetInsn(BlockNode block, InsnNode lastInsn) {
		if (lastInsn instanceof IfNode) {
			BlockNode thenBlock = ((IfNode) lastInsn).getThenBlock();
			startLabel.set(thenBlock.getId());
		} else {
			for (BlockNode successor : block.getSuccessors()) {
				startLabel.set(successor.getId());
			}
		}
	}

	public boolean isNeedStartLabel(BlockNode block) {
		return startLabel.get(block.getId());
	}

	public boolean isNeedEndGoto(BlockNode block) {
		return endGoto.get(block.getId());
	}

	// DFS sort blocks to reduce goto count
	private List<BlockNode> getSortedBlocks() {
		List<BlockNode> list = new ArrayList<>(mth.getBasicBlocks().size());
		BlockUtils.dfsVisit(mth, list::add);
		return list;
	}
}
