package libsidutils.assembler;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cml.kickass.AssemblerToolbox;
import cml.kickass.asmnode.AsmNode;
import cml.kickass.asmnode.metanodes.AsmNodeList;
import cml.kickass.asmnode.output.reciever.MainOutputReciever;
import cml.kickass.exceptions.AsmError;
import cml.kickass.libraries.Library;
import cml.kickass.libraries.MathLibrary;
import cml.kickass.libraries.MiscLibrary;
import cml.kickass.libraries.MnemonicsLibrary;
import cml.kickass.libraries.PrintLibrary;
import cml.kickass.libraries.StdConstructorLibrary;
import cml.kickass.libraries.VectorLibrary;
import cml.kickass.misc.MemoryBlock;
import cml.kickass.state.EvaluationState;
import cml.kickass.values.ConstantReferenceValue;
import cml.kickass.values.HashtableValue;
import cml.kickass.values.LabelReferenceValue;
import cml.kickass.values.SymbolScopeValue;

public class KickAssembler {

	private static final String INCLUDE_AUTOINCLUDE_ASM = "/include/autoinclude.asm";

	private static class Assembly {

		private byte data[];
		private int start;

		public Assembly(List<MemoryBlock> list) throws AsmError {
			ArrayList<MemoryBlock> memBlocks = new ArrayList<>();
			for (MemoryBlock memoryblock : list) {
				if (!memoryblock.isVirtual()) {
					memBlocks.add(memoryblock);
				}
			}
			if (memBlocks.isEmpty()) {
				System.err.println("WARNING! No data in memory!");
				data = new byte[0];
				return;
			}
			Collections.sort(memBlocks, (mem1, mem2) -> {
				int s1 = mem1.getStartAdress();
				int s2 = mem2.getStartAdress();
				if (s1 < s2)
					return -1;
				return s1 != s2 ? 1 : 0;
			});
			MemoryBlock memoryblock = (MemoryBlock) memBlocks.get(0);
			start = memoryblock.getStartAdress();
			int end = start + memoryblock.getSize();
			for (int curBlock = 1; curBlock < memBlocks.size(); curBlock++) {
				memoryblock = (MemoryBlock) memBlocks.get(curBlock);
				if (memoryblock.getStartAdress() < end) {
					throw new AsmError("Error: Memoryblock starting at $"
							+ Integer.toHexString(memoryblock.getStartAdress())
							+ " overlaps the previous block", null);
				}
				int nextEnd = memoryblock.getStartAdress()
						+ memoryblock.getSize();
				if (nextEnd > end)
					end = nextEnd;
			}
			data = new byte[2 + end - start];
			data[0] = (byte) (start & 0xff);
			data[1] = (byte) ((start >> 8) & 0xff);
			for (MemoryBlock memoryBlock : memBlocks) {
				int offset = memoryBlock.getStartAdress() - start;
				int pos = 0;
				for (byte byt : memoryBlock.getMemory()) {
					data[2 + offset + pos] = byt;
					pos++;
				}
			}
		}

		public byte[] getData() {
			return data;
		}

	}

	private ArrayList<File> sourceLibraryPath = new ArrayList<File>();
	private ArrayList<Library> libraries = new ArrayList<>();
	private EvaluationState evaluationstate;

	/**
	 * Create an assembler.
	 */
	public KickAssembler() {
		sourceLibraryPath.add(new File("."));
		libraries.add(new MathLibrary());
		libraries.add(new PrintLibrary());
		libraries.add(new VectorLibrary());
		libraries.add(new StdConstructorLibrary());
		libraries.add(new MiscLibrary());
		libraries.add(new MnemonicsLibrary());
	}

	/**
	 * @return assembly bytes of the ASM resource
	 */
	public byte[] assemble(String resource, InputStream asm,
			final Map<String, Integer> globals) {
		final HashMap<String, String> hashmap = new HashMap<String, String>();
		for (String key : globals.keySet()) {
			hashmap.put(key, String.valueOf(globals.get(key)));
		}
		evaluationstate = new EvaluationState();
		evaluationstate.setSourceLibraryPath(sourceLibraryPath);
		evaluationstate.setDtvMode(false);
		evaluationstate.setMaxMemoryAddress(65535);
		HashtableValue hashtablevalue = new HashtableValue()
				.addStringValues(hashmap);
		hashtablevalue.lock(null);
		evaluationstate.getSystemScope().getSymbols()
				.put("cmdLineVars", new ConstantReferenceValue(hashtablevalue));
		for (Library library : libraries) {
			evaluationstate.addLibrary(library);
		}
		AsmNodeList asmNodeList = new AsmNodeList();
		InputStream inputstream = cml.kickass.KickAssembler.class
				.getResourceAsStream(INCLUDE_AUTOINCLUDE_ASM);
		asmNodeList.add(AssemblerToolbox.loadAndLex(inputstream,
				INCLUDE_AUTOINCLUDE_ASM, evaluationstate, null));
		asmNodeList.add(AssemblerToolbox.loadAndLex(asm, resource,
				evaluationstate, null));
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
		MainOutputReciever mainOutputReceiver = new MainOutputReciever(8192,
				false, evaluationstate.getMaxMemoryAddress());
		asmNode.deliverOutput(mainOutputReceiver);
		mainOutputReceiver.finish();
		return new Assembly(mainOutputReceiver.getMemoryBlocks()).getData();
	}

	/**
	 * @return label values of the assembly
	 */
	public Map<String, Integer> getLabels() {
		Map<String, Integer> result = new HashMap<>();
		Iterator<Entry<String, SymbolScopeValue>> it = evaluationstate
				.getCurrentScope().getSymbols().getLocalDefinedEntities()
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, SymbolScopeValue> entry = it.next();
			SymbolScopeValue value = entry.getValue();
			if (value.getClass().equals(LabelReferenceValue.class)) {
				result.put(entry.getKey(), value.getInt(null));
			}
		}
		return result;
	}
}
