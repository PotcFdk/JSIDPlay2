package libsidutils.assembler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import kickass.AssemblerToolbox;
import kickass.common.diagnostics.IDiagnostic;
import kickass.common.errors.printers.OneLineErrorPrinter;
import kickass.common.exceptions.AsmErrorException;
import kickass.parsing.sourcelocation.SourceRange;
import kickass.pass.asmnode.AsmNode;
import kickass.pass.asmnode.metanodes.AsmNodeList;
import kickass.pass.asmnode.metanodes.AsmNodePair;
import kickass.pass.asmnode.metanodes.NamespaceNode;
import kickass.pass.asmnode.metanodes.ScopeAndSymbolPageNode;
import kickass.pass.asmnode.output.reciever.MainOutputReciever;
import kickass.pass.valueholder.ConstantValueHolder;
import kickass.pass.values.HashtableValue;
import kickass.state.EvaluationState;
import kickass.state.scope.symboltable.SymbolStatus;

public class KickAssembler {

	/**
	 * @return assembly bytes of the ASM resource
	 */
	public KickAssemblerResult assemble(String resource, InputStream asm, final Map<String, String> globals) {
		ByteArrayOutputStream result = new ByteArrayOutputStream();

		final EvaluationState state = new EvaluationState();
		try {
			state.outputMgr = (a, b) -> result;
			state.c64OutputMgr.inputfileWithoutExt = resource;
			state.parameters.outputfile = resource + ".bin";

			HashtableValue globalValues = new HashtableValue().addStringValues(globals);
			globalValues.lock((SourceRange) null);
			state.namespaceMgr.getSystemNamespace().getScope()
					.defineErrorIfExist("cmdLineVars", x -> new ConstantValueHolder(globalValues), state,
							"ERROR! cmdLineVars is already defined", (SourceRange) null)
					.setStatus(SymbolStatus.defined);

			state.prepareNewPass();
			AsmNode currentAsmNode = new ScopeAndSymbolPageNode(
					new AsmNodePair(new AsmNodeList(new ArrayList<>()).executeMetaRegistrations(state),
							new NamespaceNode(
									AssemblerToolbox.loadAndLexOrError(asm, resource, state, (SourceRange) null),
									state.namespaceMgr.getRootNamespace()).executeMetaRegistrations(state)),
					state.namespaceMgr.getSystemNamespace().getScope()).executePrepass(state);
			this.printErrorsAndTerminate(state);

			do {
				state.prepareNewPass();
				currentAsmNode = currentAsmNode.executePass(state);
				state.segmentMgr.postPassExecution();
				this.printErrorsAndTerminate(state);
				if (!state.getMadeMetaProgress() && !currentAsmNode.isFinished()) {
					state.prepareNewPass();
					state.setFailOnInvalidValue(true);
					currentAsmNode.executePass(state);
					throw new AsmErrorException(
							"Made no progress and can't solve the program.. You should have gotten an error. Contact the author!",
							(SourceRange) null);
				}
			} while (!currentAsmNode.isFinished());
			MainOutputReciever mainOutputReceiver = new MainOutputReciever(state.outputMgr, state.log);
			currentAsmNode.deliverOutput(mainOutputReceiver);
			mainOutputReceiver.finish();
			state.segmentMgr.postPassesExecution();
			this.printErrorsAndTerminate(state);
			state.c64OutputMgr.postPassExecution();
			this.printErrorsAndTerminate(state);
			state.segmentMgr.doOutputAfterPasses();
			this.printErrorsAndTerminate(state);

			Map<String, Integer> resolvedSymbols = state.scopeMgr.getResolvedSymbols().stream()
					.collect(Collectors.toMap(res -> res.name, res -> res.address, (entry1, entry2) -> entry2));
			return new KickAssemblerResult(result.toByteArray(), resolvedSymbols);
		} catch (AsmErrorException e) {
			IDiagnostic asmError = e.getError();
			asmError.setCallStack(state.callStack);
			System.err.println(OneLineErrorPrinter.instance.printError(asmError, state));
			throw new AsmErrorException(asmError, false);
		} catch (Exception e) {
			throw new RuntimeException("Internal Error!", e);
		}
	}

	private void printErrorsAndTerminate(EvaluationState evaluationState) {
		if (!evaluationState.diagnosticMgr.getErrors().isEmpty()) {
			int n = evaluationState.diagnosticMgr.getErrors().size();
			System.err.println("Got " + n + " errors while parsing:");
			for (int i = 0; i < n; ++i) {
				IDiagnostic asmError = evaluationState.diagnosticMgr.getErrors().get(i);
				System.err.println("  " + OneLineErrorPrinter.instance.printError(asmError, evaluationState));
			}
			throw new AsmErrorException(evaluationState.diagnosticMgr.getErrors().get(0), false);
		}
	}

}
