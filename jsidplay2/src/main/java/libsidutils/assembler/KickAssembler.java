package libsidutils.assembler;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cml.kickass.AssemblerToolbox;
import cml.kickass.asmnode.AsmNode;
import cml.kickass.asmnode.metanodes.AsmNodeList;
import cml.kickass.asmnode.output.reciever.MainOutputReciever;
import cml.kickass.exceptions.AsmError;
import cml.kickass.misc.MemoryBlock;
import cml.kickass.state.EvaluationState;
import cml.kickass.values.ConstantReferenceValue;
import cml.kickass.values.HashtableValue;
import cml.kickass.values.LabelReferenceValue;
import cml.kickass.values.SymbolScopeValue;

public class KickAssembler {

	private static class Assembly {

		private ByteBuffer data;
		private int start;

		public Assembly(List<MemoryBlock> memBlocks) throws AsmError {
			memBlocks.removeIf(mem -> mem.isVirtual());
			Collections.sort(memBlocks, (mem1, mem2) -> mem1.getStartAdress() - mem2.getStartAdress());
			if (memBlocks.isEmpty()) {
				throw new AsmError("Error: No data in memory!", null);
			}
			MemoryBlock memoryblock = (MemoryBlock) memBlocks.get(0);
			start = memoryblock.getStartAdress();
			int end = start + memoryblock.getSize();
			for (int curBlock = 1; curBlock < memBlocks.size(); curBlock++) {
				memoryblock = (MemoryBlock) memBlocks.get(curBlock);
				if (memoryblock.getStartAdress() < end) {
					throw new AsmError("Error: Memoryblock starting at $"
							+ Integer.toHexString(memoryblock.getStartAdress()) + " overlaps the previous block", null);
				}
				int nextEnd = memoryblock.getStartAdress() + memoryblock.getSize();
				if (nextEnd > end)
					end = nextEnd;
			}
			data = ByteBuffer.allocate(Short.BYTES + end - start).order(ByteOrder.LITTLE_ENDIAN);
			data.asShortBuffer().put((short) start);
			for (MemoryBlock memoryBlock : memBlocks) {
				data.position(Short.BYTES + memoryBlock.getStartAdress() - start);
				for (byte byt : memoryBlock.getMemory()) {
					data.put(byt);
				}
			}
		}

		public byte[] getData() {
			return data.array();
		}

	}

	private EvaluationState evaluationstate;

	/**
	 * @return assembly bytes of the ASM resource
	 */
	public byte[] assemble(String resource, InputStream asm, final Map<String, String> globals) {
		evaluationstate = new EvaluationState();
		evaluationstate.setSourceLibraryPath(new ArrayList<File>());
		evaluationstate.setDtvMode(false);
		evaluationstate.setMaxMemoryAddress(65535);
		HashtableValue hashtablevalue = new HashtableValue().addStringValues(globals);
		hashtablevalue.lock(null);
		evaluationstate.getSystemScope().getSymbols().put("cmdLineVars", new ConstantReferenceValue(hashtablevalue));
		AsmNodeList asmNodeList = new AsmNodeList();
		asmNodeList.add(AssemblerToolbox.loadAndLex(asm, resource, evaluationstate, null));
		AsmNode asmNode = asmNodeList;
		do {
			evaluationstate.prepareNewParse();
			if (evaluationstate.getPassNo() == 1) {
				asmNode.registerMetaDefinitions(evaluationstate);
				asmNode.preParse(evaluationstate);
			}
			asmNode = asmNode.parse(evaluationstate);
			if (!evaluationstate.getMadeMetaProgress() && !asmNode.isFinished()) {
				throw new AsmError("InternalError!", null);
			}
		} while (!asmNode.isFinished());
		MainOutputReciever mainOutputReceiver = new MainOutputReciever(8192, false,
				evaluationstate.getMaxMemoryAddress());
		asmNode.deliverOutput(mainOutputReceiver);
		mainOutputReceiver.finish();
		return new Assembly(mainOutputReceiver.getMemoryBlocks()).getData();
	}

	/**
	 * @return label values of the assembly
	 */
	public Map<String, Integer> getLabels() {
		Map<String, Integer> result = new HashMap<>();
		Set<Entry<String, SymbolScopeValue>> localDefines = evaluationstate.getCurrentScope().getSymbols()
				.getLocalDefinedEntities().entrySet();
		for (Entry<String, SymbolScopeValue> entry : localDefines) {
			SymbolScopeValue value = entry.getValue();
			if (value.getClass().equals(LabelReferenceValue.class)) {
				result.put(entry.getKey(), value.getInt(null));
			}
		}
		return result;
	}
}
