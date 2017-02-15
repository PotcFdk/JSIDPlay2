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
import kickass.segments.MemoryBlock;
import kickass.state.EvaluationState;
import kickass.state.scope.symboltable.SymbolStatus;
import kickass.tools.tuples.Pair;
import kickass.valueholder.ConstantValueHolder;
import kickass.values.HashtableValue;
import kickassu.errors.AsmError;
import kickassu.errors.printers.OneLineErrorPrinter;
import kickassu.exceptions.AsmErrorException;
import kickassu.parsing.sourcelocation.SourceRange;

public class KickAssembler {

	private static class Assembly {

		private ByteBuffer data;

		public Assembly(List<MemoryBlock> memBlocks) throws AsmErrorException {
			memBlocks.removeIf(mem -> mem.isVirtual());
			Collections.sort(memBlocks, (mem1, mem2) -> mem1.getStartAdress() - mem2.getStartAdress());
			if (memBlocks.isEmpty()) {
				throw new AsmErrorException("Error: No data in memory!", null);
			}
			MemoryBlock memoryblock = (MemoryBlock) memBlocks.get(0);
			int start = memoryblock.getStartAdress();
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
				int offset = Short.BYTES + memoryBlock.getStartAdress() - start;
				data.position(offset);
				data.put(memoryBlock.getMemory(), 0, memoryblock.getMemory().length);
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
		try {
			evaluationState = new EvaluationState();
			evaluationState.setMaxMemoryAddress(65635);
			HashtableValue hashtableValue = new HashtableValue().addStringValues(globals);
			hashtableValue.lock(null);
			evaluationState.getSystemNamespace().getScope()
					.defineErrorIfExist("cmdLineVars", arrReferenceValue -> new ConstantValueHolder(hashtableValue),
							evaluationState, "ERROR! cmdLineVars is already defined", null)
					.setStatus(SymbolStatus.defined);

			AsmNode asmNode = AssemblerToolbox.loadAndLexOrError(asm, resource, evaluationState, null);
			if (asmNode == null) {
				throw new RuntimeException("Parse error for assembler resource: " + resource);
			}
			asmNode = new NamespaceNode(asmNode, evaluationState.getRootNamespace());
			AsmNodeList asmNodeList = new AsmNodeList(asmNode);
			ScopeAndSymbolPageNode scopeAndSymbolPageNode = new ScopeAndSymbolPageNode(asmNodeList,
					evaluationState.getSystemNamespace().getScope());
			evaluationState.prepareNewPass();
			AsmNode asmNode2 = scopeAndSymbolPageNode.executeMetaRegistrations(evaluationState);
			asmNode2 = asmNode2.executePrepass(evaluationState);
			printErrorsAndTerminate(evaluationState);
			do {
				evaluationState.prepareNewPass();
				asmNode2 = asmNode2.executePass(evaluationState);
				evaluationState.getSegmentManager().postPass();
				if (!evaluationState.getMadeMetaProgress() && !asmNode2.isFinished()) {
					evaluationState.prepareNewPass();
					evaluationState.setFailOnInvalidValue(true);
					asmNode2.executePass(evaluationState);
					throw new AsmErrorException(
							"Made no progress and can\'t solve the program.. You should have gotten an error. Contact the author!",
							(SourceRange) null);
				}
			} while (!asmNode2.isFinished());

			MainOutputReciever mainOutputReciever = new MainOutputReciever(8192, false,
					evaluationState.getMaxMemoryAddress(), null);
			asmNode2.deliverOutput(mainOutputReciever);
			mainOutputReciever.finish();
			return new Assembly(mainOutputReciever.getSegments().get("Default")).getData();
		} catch (AsmErrorException e) {
			AsmError asmError = e.getError();
			asmError.setCallStack(evaluationState.getCallStack());
			System.err.println(OneLineErrorPrinter.instance.printError(asmError, evaluationState));
			throw new AsmErrorException(asmError);
		} catch (Exception e) {
			throw new RuntimeException("Internal Error!");
		}
	}

	private void printErrorsAndTerminate(EvaluationState evaluationState) {
		if (!evaluationState.getErrors().isEmpty()) {
			int n = evaluationState.getErrors().size();
			System.err.println("Got " + n + " errors while parsing:");
			for (int i = 0; i < n; ++i) {
				AsmError asmError = evaluationState.getErrors().get(i);
				System.err.println("  " + OneLineErrorPrinter.instance.printError(asmError, evaluationState));
			}
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
