package libsidutils.assembler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

import kickass.AssemblerToolbox;
import kickass.common.errors.AsmError;
import kickass.common.errors.printers.OneLineErrorPrinter;
import kickass.common.exceptions.AsmErrorException;
import kickass.parsing.sourcelocation.SourceRange;
import kickass.pass.asmnode.AsmNode;
import kickass.pass.asmnode.metanodes.AsmNodeList;
import kickass.pass.asmnode.metanodes.NamespaceNode;
import kickass.pass.asmnode.metanodes.ScopeAndSymbolPageNode;
import kickass.pass.asmnode.output.reciever.MainOutputReciever;
import kickass.pass.valueholder.ConstantValueHolder;
import kickass.pass.values.HashtableValue;
import kickass.state.EvaluationState;
import kickass.state.scope.symboltable.SymbolStatus;

public class KickAssembler {

	private EvaluationState evaluationState;

	/**
	 * @return assembly bytes of the ASM resource
	 */
	public KickAssemblerResult assemble(String resource, InputStream asm, final Map<String, String> globals) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			evaluationState = new EvaluationState();
			evaluationState.outputMgr = (var1, var2) -> os;
			HashtableValue hashtableValue = new HashtableValue().addStringValues(globals);
			hashtableValue.lock((SourceRange) null);
			evaluationState.namespaceMgr.getSystemNamespace().getScope()
					.defineErrorIfExist("cmdLineVars", arrReferenceValue -> new ConstantValueHolder(hashtableValue),
							evaluationState, "ERROR! cmdLineVars is already defined", (SourceRange) null)
					.setStatus(SymbolStatus.defined);

			AsmNode asmNode = AssemblerToolbox.loadAndLexOrError(asm, resource, evaluationState, (SourceRange) null);
			if (asmNode == null) {
				throw new RuntimeException("Parse error for assembler resource: " + resource);
			}
			asmNode = new NamespaceNode(asmNode, evaluationState.namespaceMgr.getRootNamespace());
			AsmNodeList asmNodeList = new AsmNodeList(asmNode);
			ScopeAndSymbolPageNode scopeAndSymbolPageNode = new ScopeAndSymbolPageNode(asmNodeList,
					evaluationState.namespaceMgr.getSystemNamespace().getScope());
			evaluationState.prepareNewPass();
			AsmNode asmNode2 = scopeAndSymbolPageNode.executeMetaRegistrations(evaluationState);
			asmNode2 = asmNode2.executePrepass(evaluationState);
			printErrorsAndTerminate(evaluationState);
			do {
				evaluationState.prepareNewPass();
				asmNode2 = asmNode2.executePass(evaluationState);
				evaluationState.segmentMgr.postPassExecution();
				if (!evaluationState.getMadeMetaProgress() && !asmNode2.isFinished()) {
					evaluationState.prepareNewPass();
					evaluationState.setFailOnInvalidValue(true);
					asmNode2 = asmNode2.executePass(evaluationState);
					throw new AsmErrorException(
							"Made no progress and can\'t solve the program. You should have gotten an error. Contact the author!",
							(SourceRange) null);
				}
			} while (!asmNode2.isFinished());
			MainOutputReciever mainOutputReciever = new MainOutputReciever(evaluationState.outputMgr,
					evaluationState.log);
			asmNode2.deliverOutput(mainOutputReciever);
			mainOutputReciever.finish();
			evaluationState.segmentMgr.postPassesExecution();
			printErrorsAndTerminate(evaluationState);
			evaluationState.c64OutputMgr.postPassExecution();
			printErrorsAndTerminate(evaluationState);
			evaluationState.segmentMgr.doOutputAfterPasses();

			Map<String, Integer> resolvedSymbols = evaluationState.scopeMgr.getResolvedSymbols().stream()
					.collect(Collectors.toMap(res -> res.name, res -> res.address, (entry1, entry2) -> entry2));
			return new KickAssemblerResult(os.toByteArray(), resolvedSymbols);
		} catch (AsmErrorException e) {
			AsmError asmError = e.getError();
			asmError.setCallStack(evaluationState.callStack);
			System.err.println(OneLineErrorPrinter.instance.printError(asmError, evaluationState));
			throw new AsmErrorException(asmError);
		} catch (Exception e) {
			throw new RuntimeException("Internal Error!", e);
		}
	}

	private void printErrorsAndTerminate(EvaluationState evaluationState) {
		if (!evaluationState.errorMgr.getErrors().isEmpty()) {
			int n = evaluationState.errorMgr.getErrors().size();
			System.err.println("Got " + n + " errors while parsing:");
			for (int i = 0; i < n; ++i) {
				AsmError asmError = evaluationState.errorMgr.getErrors().get(i);
				System.err.println("  " + OneLineErrorPrinter.instance.printError(asmError, evaluationState));
			}
			throw new AsmErrorException(evaluationState.errorMgr.getErrors().get(0));
		}
	}

}
