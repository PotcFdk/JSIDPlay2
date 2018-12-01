package libsidutils.assembler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import kickass.AssemblerToolbox;
import kickass.common.errors.AsmError;
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
		final EvaluationState var1 = new EvaluationState();
		try {
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			var1.outputMgr = (a, b) -> result;
			HashtableValue var5 = new HashtableValue().addStringValues(globals);
			var5.lock((SourceRange) null);
			var1.namespaceMgr.getSystemNamespace().getScope()
					.defineErrorIfExist("cmdLineVars", var1x -> new ConstantValueHolder(var5), var1,
							"ERROR! cmdLineVars is already defined", (SourceRange) null)
					.setStatus(SymbolStatus.defined);

			var1.prepareNewPass();
			AsmNode var15 = AssemblerToolbox.loadAndLexOrError(asm, resource, var1, (SourceRange) null);
			if (var15 == null) {
				throw new RuntimeException("Parse error for assembler resource: " + resource);
			}
			NamespaceNode var51 = new NamespaceNode(var15, var1.namespaceMgr.getRootNamespace());
			var15 = var51.executeMetaRegistrations(var1);
			AsmNodeList var52 = new AsmNodeList(new ArrayList<>());
			AsmNode var53 = var52.executeMetaRegistrations(var1);
			AsmNodePair var54 = new AsmNodePair(var53, var15);
			ScopeAndSymbolPageNode var55 = new ScopeAndSymbolPageNode(var54,
					var1.namespaceMgr.getSystemNamespace().getScope());
			AsmNode var56 = var55.executePrepass(var1);
			printErrorsAndTerminate(var1);
			do {
				var1.prepareNewPass();
				var56 = var56.executePass(var1);
				var1.segmentMgr.postPassExecution();
				printErrorsAndTerminate(var1);
				if (!var1.getMadeMetaProgress() && !var56.isFinished()) {
					var1.prepareNewPass();
					var1.setFailOnInvalidValue(true);
					var56.executePass(var1);
					throw new AsmErrorException(
							"Made no progress and can\'t solve the program. You should have gotten an error. Contact the author!",
							(SourceRange) null);
				}
			} while (!var56.isFinished());
			MainOutputReciever var25 = new MainOutputReciever(var1.outputMgr, var1.log);
			var56.deliverOutput(var25);
			var25.finish();
			var1.segmentMgr.postPassesExecution();
			printErrorsAndTerminate(var1);
			var1.c64OutputMgr.postPassExecution();
			printErrorsAndTerminate(var1);
			var1.segmentMgr.doOutputAfterPasses();
			printErrorsAndTerminate(var1);

			Map<String, Integer> resolvedSymbols = var1.scopeMgr.getResolvedSymbols().stream()
					.collect(Collectors.toMap(res -> res.name, res -> res.address, (entry1, entry2) -> entry2));
			return new KickAssemblerResult(result.toByteArray(), resolvedSymbols);
		} catch (AsmErrorException e) {
			AsmError asmError = e.getError();
			asmError.setCallStack(var1.callStack);
			System.err.println(OneLineErrorPrinter.instance.printError(asmError, var1));
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
