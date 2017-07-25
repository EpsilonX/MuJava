package mujava.app;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.runner.notification.Failure;

import mujava.api.Configuration;
import mujava.api.MutationOperator;
import mujava.generations.GenerationsGoalTester;
import mujava.generations.GenerationsInformation;
import mujava.generations.Generator;
import mujava.generations.GoalTester;
import mujava.generations.RequestGenerator;
import mujava.generations.SameRequestGenerator;

/**
 * This class execute all commands either from console or from a GUI
 * and return the results to be handled by the caller
 * @author Simon Emmanuel Gutierrez Brida
 */
public class Core {
	
	/**
	 * Option to enable/disable toughness analysis, which is a relation of failed tests and total tests
	 * for each mutant.
	 * <p>
	 * this option is disabled by default
	 */
	public static final String ENABLE_TOUGHNESS = "Core_toughness";
	
	public static String SEPARATOR = fixBackslash(FileSystems.getDefault().getSeparator());
	private static Core instance = null;
	private static String inputDir;
	private static String outputDir;
	private String inputBinDir;
	private Map<String, List<String>> mutantsFolders;
	private Exception error;
	private MutationScore ms;
	//private int generation = -1 ;
	private List<MutantInfo> lastGeneration = null;
	public static boolean fullVerbose = false;
	public static boolean showSurvivingMutants = false;
	public static boolean useExternalJUnitRunner = false;
	public static boolean useParallelJUnitRunner = false;
	public static int parallelJUnitRunnerThreads;
	public static final int mujavappVersion = 20172805;
	
	private float totalToughness;
	private float totalMutants;

	private float totalKilledMutantsToughness;
	private float totalKilledMutants;
	
	public static Core newInstance(String inputDirP, String outputDirP) {
		if (instance == null) {
			instance = new Core(inputDirP, outputDirP);
		}
		if ((inputDir == null && inputDirP != null) || (inputDir != null && !inputDir.equals(inputDirP))) {
			inputDir = inputDirP;
		}
		if ((outputDir == null && outputDirP != null) || (outputDir != null && !outputDir.equals(outputDirP))) {
			outputDir = outputDirP;
		}
		return instance;
	}
	
	private static String fixBackslash(String orig) {
		if (orig == "\\") {
			return "\\\\";
		} else {
			return orig;
		}
	}

	public void setMutationScore(MutationScore ms) {
		this.ms = ms;
	}
	
	public void setInputBinDir(String binDir) {
		this.inputBinDir = binDir;
	}
	
	private Core(String inputDir, String outputDir) {
		Core.inputDir = inputDir;
		Core.outputDir = outputDir;
	}
	
	public boolean generateMutants(String className, String[] methods, MutationOperator[] mutOps) {
		return generateMutants(className, methods, mutOps, 1);
	}
	
	public boolean generateMutants(String className, String[] methods, MutationOperator[] mutOps, int generation) {
		this.error = null;
		this.mutantsFolders = null;
		if (methods == null) {
			this.error = new IllegalArgumentException("mujava.app.Core#generateMutantsForGeneration: methods param is null");
			return false;
		}
		String classNameAsPath = className.replaceAll("\\.", SEPARATOR);
		MutationRequest originalRequest = new MutationRequest(classNameAsPath, methods, mutOps, Core.inputDir, Core.outputDir);
		GoalTester goalTester = new GenerationsGoalTester(generation);
		RequestGenerator requestGenerator = new SameRequestGenerator(originalRequest);
		Generator.useLowMemoryMode(true);
		Generator generator = new Generator(requestGenerator, goalTester, (Core.fullVerbose?Generator.VERBOSE_LEVEL.FULL_VERBOSE:Generator.VERBOSE_LEVEL.NO_VERBOSE));
		try {
			GenerationsInformation generationsInfo= generator.generate(false, true);
			if (Core.fullVerbose) System.out.println(generationsInfo.showBasicInformation());
			this.mutantsFolders = generator.getMutantsFolderForGeneration(generation);
			//this.generation = generation;
			this.lastGeneration = generationsInfo.getLastGeneration();
		} catch (Exception e) {
			this.error = e;
		}
		return this.error == null;
	}
	
	public List<Method> getMethods(String className) {
		Class<?> clazz = loadClass(className);
		List<Method> methods = new LinkedList<Method>();
		if (clazz != null) {
			methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
		}
		return methods;
	}
	
	
	private Class<?> loadClass(String className) {
		File file = new File(this.inputBinDir);
		Class<?> clazz = null;
		String classToLoad = className;
		try {
		    URL url = file.toURI().toURL();
		    URL[] urls = new URL[]{url};
		    
		    URLClassLoader cl = new URLClassLoader(urls);

		    clazz = cl.loadClass(classToLoad);
		    cl.close();
		    
		} catch (MalformedURLException e) {
			this.error = e;
		} catch (ClassNotFoundException e) {
			this.error = e;
		} catch (IOException e) {
			this.error = e;
		}
		return clazz;
	}
	
	public Exception lastError() {
		return this.error;
	}
	
	public Map<String, List<String>> lastMutantsFolder() {
		return this.mutantsFolders;
	}
	
	public String getInputDir() {
		return Core.inputDir;
	}
	
	public String getOutputDir() {
		return Core.outputDir;
	}
	
	public float calculateMutationScore(String[] testClasses, String className) {
		if (!useExternalJUnitRunner && !useParallelJUnitRunner) {
			return calculateMutationScore_internalRunner(testClasses, className);
		} else {
			String original = getFileToMutate(className);
			float ms = -1;
			if (deactivateOriginal(original)) {
				ms = calculateMutationScoreUsingExternalRunner(testClasses, className, useParallelJUnitRunner);
				if (!reactivateOriginal(original)) ms = -1;
			}
			return ms;
		}
	}
	
	private String getFileToMutate(String className) {
		String classNameAsPath = className.replaceAll("\\.", SEPARATOR);
		String fileToMutateName = classNameAsPath + ".class";
		return this.inputBinDir + File.separator + fileToMutateName;
	}
	
	private boolean deactivateOriginal(String path) {
		File f = new File(path);
		if (f.exists() && f.isFile()) {
			return f.renameTo(new File(f.getAbsolutePath()+".bak"));
		} else {
			return false;
		}
	}
	
	private boolean reactivateOriginal(String path) {
		File f = new File(path+".bak");
		if (f.exists() && f.isFile()) {
			return f.renameTo(new File(f.getAbsolutePath().replace(".bak", "")));
		} else {
			return false;
		}
	}
	
	private float calculateMutationScore_internalRunner(String[] testClasses, String className) {
		if (this.lastGeneration == null) {
			this.error = new IllegalStateException("There are no recorder mutants in the last generation");
			return -1;
		}
		List<String> survivingMutantsPaths = new LinkedList<>();
		int failedToCompile = 0;
		int mutantsKilled = 0;
		int mutants = 0;
		int timedOut = 0;
		for (MutantInfo mut : this.lastGeneration) {
			mutants++;
			String pathToFile = mut.getPath();
			if (!ms.compile(pathToFile).compilationSuccessful()){
				System.out.println("File : " + pathToFile + " didn't compile\n");
				failedToCompile++;
				continue;
			}
			boolean killed = false;
			List<TestResult> results = ms.runTestsWithMutants(Arrays.asList(testClasses), mut);
			if (results == null) {
				System.out.println("An error ocurred while running tests for mutants");
				System.out.println(ms.getLastError()!=null?ms.getLastError().toString():"no exception to display, contact your favorite mujava++ developer");
				return -1;
			}
			int runnedTestsCount = 0;
			int totalFailures = 0;
			for (TestResult r : results) {
				System.out.println(r.toString()+"\n");
				runnedTestsCount += r.getRunnedTestsCount();
				totalFailures += r.getTotalFailures();
				if (!r.wasSuccessful()) {
					if (r.getTimedoutTests() > 0) timedOut++;
					for (Failure f : r.getFailures()) {
						if (Core.fullVerbose) System.out.println("test : " + f.getTestHeader());
						if (Core.fullVerbose) System.out.println("failure description: " + f.getDescription());
						if (Core.fullVerbose && !(f.getException() instanceof java.lang.AssertionError)) System.out.println("exception: " + f.getException());
						if (Core.fullVerbose && !(f.getException() instanceof java.lang.AssertionError)) System.out.println("trace: " + f.getTrace());
					}
				}
				if (!killed && !r.wasSuccessful()) killed = true;
			}
			if (toughnessAnalysis()) {
				float toughness = 1.0f - ((totalFailures * 1.0f) / (runnedTestsCount * 1.0f));
				this.addToughnessValue(toughness);
				System.out.println("Toughness: " + toughness + " [failed : " + totalFailures + " | total : " + runnedTestsCount + "]");
				if (killed) {
					this.addKilledMutantsToughnessValue(toughness);
				}
				System.out.println();
			}
			if (killed) mutantsKilled++;
			if (!killed && Core.showSurvivingMutants) {
				survivingMutantsPaths.add(pathToFile);
			}
			
		}
		return mutationAnalysisResults(mutants, failedToCompile, mutantsKilled, timedOut, survivingMutantsPaths);
	}
	
	public float calculateMutationScoreUsingExternalRunner(String[] testClasses, String className, boolean parallelize) {
		if (this.lastGeneration == null) {
			this.error = new IllegalStateException("There are no recorder mutants in the last generation");
			return -1;
		}
		if (parallelize) {
			return calculateMutationScoreUsingExternalRunner_parallel(testClasses, className);
		} else {
			return calculateMutationScoreUsingExternalRunner_sequential(testClasses, className);
		}
	}
	
	private float calculateMutationScoreUsingExternalRunner_sequential(String[] testClasses, String className) {
		List<String> survivingMutantsPaths = new LinkedList<>();
		int failedToCompile = 0;
		int mutantsKilled = 0;
		int mutants = 0;
		int timedOut = 0;
		for (MutantInfo mut : this.lastGeneration) {
			mutants++;
			String pathToFile = mut.getPath();
			if (!ms.compile(pathToFile).compilationSuccessful()){
				System.out.println("File : " + pathToFile + " didn't compile\n");
				failedToCompile++;
				continue;
			}
			boolean killed = false;
			
			ExternalJUnitResult results = ms.runTestsWithMutantsUsingExternalRunner(Arrays.asList(testClasses), mut);
			if (!results.testsRunSuccessful()) {
				System.out.println("An error ocurred while running tests for mutants");
				System.out.println(results.error()!=null?results.error().toString():"no exception to display, contact your favorite mujava++ developer");
				return -1;
			}
			int runnedTestsCount = 0;
			int totalFailures = 0;
			for (TestResult r : results.testResults()) {
				System.out.println(r.toString()+"\n");
				runnedTestsCount += r.getRunnedTestsCount();
				totalFailures += r.getTotalFailures();
				if (!r.wasSuccessful()) {
					if (r.getTimedoutTests() > 0) timedOut++;
					for (Failure f : r.getFailures()) {
						if (Core.fullVerbose) System.out.println("test : " + f.getTestHeader());
						if (Core.fullVerbose) System.out.println("failure description: " + f.getDescription());
						if (Core.fullVerbose && !(f.getException() instanceof java.lang.AssertionError)) System.out.println("exception: " + f.getException());
						if (Core.fullVerbose && !(f.getException() instanceof java.lang.AssertionError)) System.out.println("trace: " + f.getTrace());
					}
				}
				if (!killed && !r.wasSuccessful()) killed = true;
			}
			if (toughnessAnalysis()) {
				float toughness = 1.0f - ((totalFailures * 1.0f) / (runnedTestsCount * 1.0f));
				this.addToughnessValue(toughness);
				System.out.println("Toughness: " + toughness + " [failed : " + totalFailures + " | total : " + runnedTestsCount + "]");
				if (killed) {
					this.addKilledMutantsToughnessValue(toughness);
				}
				System.out.println();
			}
			if (killed) mutantsKilled++;
			if (!killed && Core.showSurvivingMutants) {
				survivingMutantsPaths.add(pathToFile);
			}
			
		}
		return mutationAnalysisResults(mutants, failedToCompile, mutantsKilled, timedOut, survivingMutantsPaths);
	}
	
	private float calculateMutationScoreUsingExternalRunner_parallel(String[] testClasses, String className) {
		List<String> survivingMutantsPaths = new LinkedList<>();
		int failedToCompile = 0;
		int mutantsKilled = 0;
		int mutants = 0;
		int timedOut = 0;
		List<Future<ExternalJUnitParallelRunnerResult>> junitExternalRunnerTasks = new LinkedList<>();
		ExecutorService es = Executors.newFixedThreadPool(Core.parallelJUnitRunnerThreads);
		for (MutantInfo mut : this.lastGeneration) {
			mutants++;
			ExternalJUnitParallelRunner externalRunner = new ExternalJUnitParallelRunner(Arrays.asList(testClasses), mut, this.ms);
			Future<ExternalJUnitParallelRunnerResult> newTask = es.submit(externalRunner);
			junitExternalRunnerTasks.add(newTask);
		}
		for (int i = 0; i < junitExternalRunnerTasks.size(); i++) {
			Future<ExternalJUnitParallelRunnerResult> t = junitExternalRunnerTasks.get(i);
			ExternalJUnitParallelRunnerResult externalJPRResults;
			try {
				externalJPRResults = t.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				return -1;
			}
			System.out.println("Testing mutant : "+externalJPRResults.getMutant().getPath()+'\n');
			
			if (!externalJPRResults.compilationResults().compilationSuccessful()) {
				System.err.println("File : " + externalJPRResults.getMutant().getPath() + " didn't compile\n");
				System.err.print("Compilation error:\n" + externalJPRResults.compilationResults().error().getMessage());
				failedToCompile++;
				continue;
			} else if (externalJPRResults.compilationResults().getWarnings() != null) {
				System.err.println("Warnings\n"+externalJPRResults.compilationResults().getWarnings());
			}
			boolean killed = false;
			ExternalJUnitResult testResults = externalJPRResults.testResults();
			if (!testResults.testsRunSuccessful()) {
				System.err.println("An error ocurred while running tests for mutants");
				System.err.println(testResults.error().getMessage());
				return -1;
			}
			int runnedTestsCount = 0;
			int totalFailures = 0;
			for (TestResult r : testResults.testResults()) {
				System.out.println(r.toString()+"\n");
				runnedTestsCount += r.getRunnedTestsCount();
				totalFailures += r.getTotalFailures();
				if (!r.wasSuccessful()) {
					if (r.getTimedoutTests() > 0) timedOut++;
					for (Failure f : r.getFailures()) {
						if (Core.fullVerbose) System.out.println("test : " + f.getTestHeader());
						if (Core.fullVerbose) System.out.println("failure description: " + f.getDescription());
						if (Core.fullVerbose && !(f.getException() instanceof java.lang.AssertionError)) System.out.println("exception: " + f.getException());
						if (Core.fullVerbose && !(f.getException() instanceof java.lang.AssertionError)) System.out.println("trace: " + f.getTrace());
					}
				}
				if (!killed && !r.wasSuccessful()) killed = true;
			}
			if (toughnessAnalysis()) {
				float toughness = 1.0f - ((totalFailures * 1.0f) / (runnedTestsCount * 1.0f));
				this.addToughnessValue(toughness);
				System.out.println("Toughness: " + toughness + " [failed : " + totalFailures + " | total : " + runnedTestsCount + "]");
				if (killed) {
					this.addKilledMutantsToughnessValue(toughness);
				}
				System.out.println();
			}
			if (killed) mutantsKilled++;
			if (!killed && Core.showSurvivingMutants) {
				survivingMutantsPaths.add(externalJPRResults.getMutant().getPath());
			}
		}
		es.shutdown();
		return mutationAnalysisResults(mutants, failedToCompile, mutantsKilled, timedOut, survivingMutantsPaths);
		
	}
	
	private static class ExternalJUnitParallelRunnerResult {
		private CompilationResult cr;
		private ExternalJUnitResult results;
		private MutantInfo mut;
		
		
		public ExternalJUnitParallelRunnerResult(CompilationResult cr, ExternalJUnitResult testResults, MutantInfo mut) {
			this.cr = cr;
			this.results = testResults;
			this.mut = mut;
		}
		
		public CompilationResult compilationResults() {
			return this.cr;
		}
		
		public ExternalJUnitResult testResults() {
			return this.results;
		}
		
		public MutantInfo getMutant() {
			return this.mut;
		}
		
		
	}
	
	private static class ExternalJUnitParallelRunner implements Callable<ExternalJUnitParallelRunnerResult> {
		
		private List<String> testClasses;
		private MutantInfo mut;
		private MutationScore ms;
		
		
		public ExternalJUnitParallelRunner(List<String> testClasses, MutantInfo mut, MutationScore ms) {
			this.testClasses = testClasses;
			this.mut = mut;
			this.ms = ms;
		}
		
		
		@Override
		public ExternalJUnitParallelRunnerResult call() throws Exception {
			String pathToFile = mut.getPath();
			CompilationResult cresult = ms.compile(pathToFile);
			ExternalJUnitResult results = null;
			if (cresult == null) {
				results = new ExternalJUnitResult(new Exception("Compilation error while compiling mutant "+mut.getPath()));
			} else if (cresult.compilationSuccessful()) {
				results = ms.runTestsWithMutantsUsingExternalRunner(testClasses, mut);
		  	}
			System.out.println("Mutant analysis for : " + mut.getPath() + " finished");
			return new ExternalJUnitParallelRunnerResult(cresult, results, this.mut);
		}

	
		
	}
	
	private float mutationAnalysisResults(int mutants, int failedToCompile, int mutantsKilled, int timedOut, List<String> survivingMutantsPaths) {
		System.out.println("Mutants : "+ mutants + " | didn't compile : " + failedToCompile + " | mutants killed by tests : "+ mutantsKilled + " | surviving mutants : " + (mutants-failedToCompile-mutantsKilled) + " | total tests that timedout : " + timedOut + " | mutation score : "+((mutantsKilled+failedToCompile)*100.0)/mutants+ " | mutation score (only compiling mutants) : " + (mutantsKilled*100.0)/(mutants-failedToCompile) + '\n');
		if (Core.showSurvivingMutants) {
			System.out.println("Surviving mutants paths:\n");
			for (String sm : survivingMutantsPaths) {
				System.out.println(sm);
			}
		}
		if (toughnessAnalysis()) {
			System.out.println("Average toughness : " + this.averageToughness());
			System.out.println("Average killed mutants toughness : " + this.averageKilledMutantsToughness());
		}
		return ((mutantsKilled+failedToCompile)*(float)100.0)/mutants;
	}
	
	public static boolean toughnessAnalysis() {
		if (Configuration.argumentExist(ENABLE_TOUGHNESS)) {
			return (Boolean) Configuration.getValue(ENABLE_TOUGHNESS);
		} else {
			return false;
		}
	}
	
	private void addToughnessValue(float t) {
		this.totalToughness += t;
		this.totalMutants++;
	}
	
	private void addKilledMutantsToughnessValue(float t) {
		this.totalKilledMutantsToughness += t;
		this.totalKilledMutants++;
	}
	
	private float averageToughness() {
		return this.totalToughness / this.totalMutants;
	}
	
	private float averageKilledMutantsToughness() {
		return this.totalKilledMutantsToughness / this.totalKilledMutants;
	}
	
	@SuppressWarnings("deprecation")
	public static void killStillRunningJUnitTestcaseThreads() {
	    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
	    for (Thread thread : threadSet) {
	        if (!(thread.isDaemon())) {
	            final StackTraceElement[] threadStackTrace = thread.getStackTrace();
	            if (threadStackTrace.length > 1) {
	                StackTraceElement firstMethodInvocation = threadStackTrace[threadStackTrace.length - 1];
	                if (firstMethodInvocation.getClassName().startsWith("org.junit")) {
	                    // HACK: must use deprecated method
	                    thread.stop();
	                }
	            }
	        }
	    }
	}

}
