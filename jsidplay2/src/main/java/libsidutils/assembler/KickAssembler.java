package libsidutils.assembler;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kickass.AssemblerToolbox;
import kickass.asmnode.AsmNode;
import kickass.asmnode.metanodes.AsmNodeList;
import kickass.asmnode.metanodes.NamespaceNode;
import kickass.asmnode.metanodes.ScopeAndSymbolPageNode;
import kickass.asmnode.output.reciever.MainOutputReciever;
import kickass.misc.MemoryBlock;
import kickass.state.EvaluationState;
import kickass.state.scope.symboltable.SymbolStatus;
import kickass.tools.tuples.Pair;
import kickass.values.ConstantReferenceValue;
import kickass.values.HashtableValue;
import kickassu.configuration.parameters.KickAssemblerParameters;
import kickassu.errors.AsmError;
import kickassu.errors.printers.OneLineErrorPrinter;
import kickassu.errors.printers.StackTraceErrorPrinter;
import kickassu.exceptions.AsmErrorException;

public class KickAssembler {

	private static class Assembly {

		private ByteBuffer data;
		private int start;

		public Assembly(List<MemoryBlock> memBlocks) throws AsmErrorException {
			memBlocks.removeIf(mem -> mem.isVirtual());
			Collections.sort(memBlocks, (mem1, mem2) -> mem1.getStartAdress() - mem2.getStartAdress());
			if (memBlocks.isEmpty()) {
				throw new AsmErrorException("Error: No data in memory!", null);
			}
			MemoryBlock memoryblock = (MemoryBlock) memBlocks.get(0);
			start = memoryblock.getStartAdress();
			int end = start + memoryblock.getSize();
			for (int curBlock = 1; curBlock < memBlocks.size(); curBlock++) {
				memoryblock = (MemoryBlock) memBlocks.get(curBlock);
				if (memoryblock.getStartAdress() < end) {
					throw new AsmErrorException("Error: Memoryblock starting at $"
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

	private EvaluationState evaluationState;

	/**
	 * @return assembly bytes of the ASM resource
	 */
	public byte[] assemble(String resource, InputStream asm, final Map<String, String> globals) {
		evaluationState = new EvaluationState();
		evaluationState.setMaxMemoryAddress(65635);
		try {
			KickAssemblerParameters kickAssemblerParameters = evaluationState.getKickAssemblerParams();
			HashtableValue hashtableValue = new HashtableValue().addStringValues(globals);
			hashtableValue.lock(null);
			evaluationState.getSystemNamespace().getScope()
					.defineErrorIfExist("cmdLineVars", arrReferenceValue -> new ConstantReferenceValue(hashtableValue),
							evaluationState, "ERROR! cmdLineVars is already defined", null)
					.setStatus(SymbolStatus.defined);

			AsmNode asmNode = AssemblerToolbox.loadAndLexOrError(asm, resource, evaluationState, null);
			if (asmNode == null) {
				return null;
			}
			asmNode = new NamespaceNode(asmNode, evaluationState.getRootNamespace());
			AsmNodeList asmNodeList = new AsmNodeList(asmNode);
			ScopeAndSymbolPageNode scopeAndSymbolPageNode = new ScopeAndSymbolPageNode(asmNodeList,
					evaluationState.getSystemNamespace().getScope());
			evaluationState.prepareNewParse();
			AsmNode asmNode2 = scopeAndSymbolPageNode.executeMetaRegistrations(evaluationState);
			asmNode2 = asmNode2.executePrepass(evaluationState);
			printErrorsAndTerminate(evaluationState);
			do {
				evaluationState.prepareNewParse();
				asmNode2 = asmNode2.executePass(evaluationState);
				if (evaluationState.getMadeMetaProgress() || asmNode2.isFinished())
					continue;
				evaluationState.prepareNewParse();
				evaluationState.setFailOnInvalidValue(true);
				asmNode2 = asmNode2.executePass(evaluationState);
				throw new AsmErrorException(
						"Made no progress and cant solve the program.. You should have gotten an error. Contact the author!",
						null);
			} while (!asmNode2.isFinished());

			MainOutputReciever mainOutputReciever = new MainOutputReciever(8192,
					kickAssemblerParameters.allowFileOutput, evaluationState.getMaxMemoryAddress());
			asmNode2.deliverOutput(mainOutputReciever);
			mainOutputReciever.finish();
			return new Assembly(mainOutputReciever.getMemoryBlocks()).getData();

		} catch (AsmErrorException e) {
			AsmError asmError = e.getError();
			asmError.setCallStack(evaluationState.getCallStack());
			System.err.println(StackTraceErrorPrinter.instance.printError(asmError, evaluationState));
		} catch (Exception e) {
			System.err.println(e);
		}
		throw new AsmErrorException("Internal Error!");
	}

	private void printErrorsAndTerminate(EvaluationState evaluationState) {
		if (!evaluationState.getErrors().isEmpty()) {
			int n = evaluationState.getErrors().size();
			System.err.println("Got " + n + " errors while parsing:");
			for (int i = 0; i < n; ++i) {
				AsmError asmError = evaluationState.getErrors().get(i);
				System.err.println("  " + OneLineErrorPrinter.instance.printError(asmError, evaluationState));
			}
			System.err.println();
			throw new AsmErrorException(evaluationState.getErrors().get(0));
		}
	}

	/**
	 * @return label values of the assembly
	 */
	public Map<String, Integer> getLabels() {
		Map<String, Integer> result = new HashMap<>();
		List<Pair<String, Integer>> localDefines = evaluationState.getResolvedSymbols();
		for (Pair<String, Integer> entry : localDefines) {
			result.put(entry.getA(), entry.getB());
		}
		return result;
	}
}
