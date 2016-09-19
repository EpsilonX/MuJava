package test.mujava2.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.mutable.MutableBoolean;

import mujava.OpenJavaException;
import mujava.api.MutantsInformationHolder;
import mujava.app.Core;
import mujava.app.MutationInformation;
import mujava.generations.GenerationsGoalTester;
import mujava.generations.GenerationsInformation;
import mujava.generations.Generator;
import mujava.generations.GoalTester;
import mujava.generations.RequestGenerator;
import mujava.generations.SameRequestGenerator;
import mujava.util.JustCodeDigest;
import mujava2.api.mutator.MutationRequest;
import mujava2.api.program.JavaAST;
import mujava2.api.program.MutatedAST;
import openjava.ptree.ParseTreeException;
import test.mujava2.utils.log.Log;

public class TestingTools {
	
	private MutationRequest req;
	private String outDirApi1;
	private String outDirApi2;
	
	private static boolean verbose = false;
	
	private Map<byte[], String> hashesApi1;
	private Map<byte[], String> hashesApi2;
	
	public TestingTools (MutationRequest req, String outputDirApi1, String outputDirApi2) {
		this.req = req;
		this.outDirApi1 = outputDirApi1;
		this.outDirApi2 = outputDirApi2;
	}
	
	
	public boolean compareApisMutants() {
		Log.getLog().incLevel();
		Log.getLog().publish("compareApisMutants() using request\n"+this.req.toString()+"\nIn directories: " + this.outDirApi1 + "(old API) and " + this.outDirApi2 + "(new API)" +"\n", this.getClass());
		MutableBoolean result = new MutableBoolean(true);
		File outDirApi2 = new File(this.outDirApi2);
		if (outDirApi2.exists()) {
			Log.getLog().publish("Directory " + result.toString() + " already exist", this.getClass());
			result.setValue(false);
		} else {
			if (!generateMutantsNewApi(outDirApi2)) {
				Log.getLog().publish("Error while generating mutants with new API", this.getClass());
				result.setValue(false);
			} else {
				MutantsInformationHolder.resetMainHolder();
				GenerationsInformation ginfo = generateMutantsOldAPI(true);
				if (ginfo == null) {
					Log.getLog().publish("Error while generating mutants with old API", this.getClass());
					result.setValue(false);
				} else {
					this.hashesApi1 = collectJavaFileHashesInDir(this.outDirApi1);
					if (this.hashesApi1 == null) {
						Log.getLog().publish("Error while collecting hashes from mutants in " + this.outDirApi1, this.getClass());
						result.setValue(false);
					} else {
						if (!checkGeneratedMutants(this.hashesApi1, this.hashesApi2)) {
							Log.getLog().publish("Mutants generated by new and old APIs are not the same", this.getClass());
							result.setValue(false);
						} else {
							Log.getLog().publish("Mutants generated by new and old APIs are the same", this.getClass());
						}
					}
				}
			}
		}
		Log.getLog().decLevel();
		return result.booleanValue();
	}
	
	public boolean compareApisLastMutantGeneration() {
		Log.getLog().publish("compareApisLastMutantGeneration() using request\n"+this.req.toString()+"\nIn directory: " + this.outDirApi1 + "\n", this.getClass());
		throw new UnsupportedOperationException("Not yet implemented!");
	}
	
	public boolean compareApisMutations() {
		Log.getLog().incLevel();
		Log.getLog().publish("compareApisMutations() using request\n"+this.req.toString()+"\nIn directories: " + this.outDirApi1 + "(old API) and " + this.outDirApi2 + "(new API)" +"\n", this.getClass());	
		MutableBoolean result = new MutableBoolean(true);
		File outDirApi2 = new File(this.outDirApi2);
		if (outDirApi2.exists()) {
			Log.getLog().publish("Directory " + result.toString() + " already exist", this.getClass());
			result.setValue(false);
		} else {
			if (!generateAndWriteMutationsWithNewApi(outDirApi2)) {
				Log.getLog().publish("Error while generating mutations with new API", this.getClass());
				result.setValue(false);
			} else {
				MutantsInformationHolder.resetMainHolder();
				GenerationsInformation ginfo = generateMutantsOldAPI(true);
				if (ginfo == null) {
					Log.getLog().publish("Error while generating mutants with old API", this.getClass());
					result.setValue(false);
				} else {
					this.hashesApi1 = collectJavaFileHashesInDir(this.outDirApi1);
					if (this.hashesApi1 == null) {
						Log.getLog().publish("Error while collecting hashes from mutants in " + this.outDirApi1, this.getClass());
						result.setValue(false);
					} else {
						if (!checkGeneratedMutants(this.hashesApi1, this.hashesApi2)) {
							Log.getLog().publish("Mutations generated by new and old APIs are not the same", this.getClass());
							result.setValue(false);
						} else {
							Log.getLog().publish("Mutations generated by new and old APIs are the same", this.getClass());
						}
					}
				}
			}
		}
		Log.getLog().decLevel();
		return result.booleanValue();
	}
	
	public static void setVerbose(boolean v) {
		TestingTools.verbose = v;
	}
	
	private boolean generateMutantsNewApi(File outDirApi2) {
		Log.getLog().incLevel();
		Log.getLog().publish("About to generate mutants with new API", this.getClass());
		try {
			List<MutatedAST> newApiMutants = mujava2.api.Api.generateMutants(this.req);
			this.hashesApi2 = new TreeMap<>(new ByteArrayComparator());
			for (MutatedAST mast : newApiMutants) {
				byte[] hash = mast.writeInFolder(outDirApi2);
				String file = mast.lastOutputFile().getAbsolutePath().toString();
				String repeatedFile = checkDuplicateHash(hash, this.hashesApi2);
				if (repeatedFile != null) {
					Log.getLog().publish("Duplicate files found", new Exception("Same hashes for files " + file + " and " + repeatedFile), this.getClass());
					Log.getLog().decLevel();
					return false;
				} else {
					this.hashesApi2.put(hash, file);
				}
			}
		} catch (OpenJavaException | ParseTreeException e) {
			Log.getLog().publish("Error while generating mutants with mujava2 Api", e, this.getClass());
			Log.getLog().decLevel();
			return false;
		} catch (IOException e) {
			Log.getLog().publish("Error while writing mutant with mujava2 api", e, this.getClass());
			Log.getLog().decLevel();
			return false;
		}
		Log.getLog().publish("Generated mutants with new API", this.getClass());
		Log.getLog().decLevel();
		return true;
	}
	
	private boolean generateAndWriteMutationsWithNewApi(File outDirApi2) {
		Log.getLog().incLevel();
		Log.getLog().publish("About to generate and write mutations with new API", this.getClass());
		try {
			JavaAST ast = JavaAST.fromFile(this.req.getLocation(), this.req.getClassToMutate());
			List<List<MutationInformation>> mutations = mujava2.api.Api.generateMutationGenerations(ast, this.req);
			if (!writeMutations(ast, mutations, outDirApi2)) {
				Log.getLog().publish("Error while writing mutations using mujava2 api", this.getClass());
				Log.getLog().decLevel();
				return false;
			} else {
				Log.getLog().publish("Mutations written to " + outDirApi2.getAbsolutePath() + " using mujava2 api", this.getClass());
			}
		} catch (OpenJavaException | ParseTreeException e) {
			Log.getLog().publish("Error while generating mutations with mujava2 Api", e, this.getClass());
			Log.getLog().decLevel();
			return false;
		}
		Log.getLog().decLevel();
		return true;
	}
	
	private boolean writeMutations(JavaAST ast, List<List<MutationInformation>> mutations, File outDir) {
		Log.getLog().incLevel();
		Log.getLog().publish("About to write mutations to " + outDir.getAbsolutePath() + " with mujava2 api", this.getClass());
		this.hashesApi2 = new TreeMap<>(new ByteArrayComparator());
		int currGen = 1;
		for (List<MutationInformation> mutationsPerGeneration : mutations) {
			File genOutDir = new File(outDir.toPath().resolve("generation-"+currGen).toString());
			Log.getLog().publish("Writtting generation "+ currGen + " to " + genOutDir.getAbsolutePath(), this.getClass());
			for (MutationInformation minfo : mutationsPerGeneration) {
				Log.getLog().incLevel();
				Log.getLog().publish("Writting mutant " + minfo.toString(), this.getClass());
				MutatedAST mut = new MutatedAST(ast, minfo);
				try {
					byte[] hash = mut.writeInFolder(genOutDir);
					File outFile = mut.lastOutputFile();
					if (this.hashesApi2.containsKey(hash)) {
						Log.getLog().publish("Repeated mutants " + outFile.getAbsolutePath() + " and " + this.hashesApi2.get(hash), this.getClass());
						Log.getLog().decLevel();
						return false;
					} else {
						this.hashesApi2.put(hash, outFile.getAbsolutePath());
					}
				} catch (IOException | ParseTreeException e) {
					Log.getLog().publish("Failed to write mutant", e, this.getClass());
					Log.getLog().decLevel();
					return false;
				}
				Log.getLog().publish("Mutant written", this.getClass());
				Log.getLog().decLevel();
			}
			currGen++;
		}
		Log.getLog().publish("All mutations written using mujava2 api", this.getClass());
		Log.getLog().decLevel();
		return true;
	}
	
	private GenerationsInformation generateMutantsOldAPI(boolean lowMemoryMode) {
		Log.getLog().incLevel();
		Log.getLog().publish("About to generate mutants with old API", this.getClass());
		mujava.app.MutationRequest oldReq = this.req.transformToOldRequest();
		oldReq.outputDir = this.outDirApi1;
		oldReq.clazz = oldReq.clazz.replaceAll("\\.", Core.SEPARATOR);
		GoalTester goalTester = new GenerationsGoalTester(this.req.getGenerations());
		RequestGenerator requestGenerator = new SameRequestGenerator(oldReq);
		Generator.useLowMemoryMode(lowMemoryMode);
		Generator generator = new Generator(requestGenerator, goalTester, (TestingTools.verbose?Generator.VERBOSE_LEVEL.FULL_VERBOSE:Generator.VERBOSE_LEVEL.NO_VERBOSE));
		try {
			GenerationsInformation generationsInfo= generator.generate(false, true);
			if (TestingTools.verbose) System.out.println(generationsInfo.showBasicInformation());
			Log.getLog().publish("Generated mutants with old API", this.getClass());
			Log.getLog().decLevel();
			return generationsInfo;
		} catch (Exception e) {
			Log.getLog().publish("Exception while generating mutants with old API", e, this.getClass());
			Log.getLog().decLevel();
			return null;
		}
	}
	
	private Map<byte[], String> collectJavaFileHashesInDir(String dir) {
		Log.getLog().incLevel();
		Log.getLog().publish("Collecting mutant hashes from directory : " + dir, this.getClass());
		MutableBoolean error = new MutableBoolean(false);
		final Map<byte[], String> hashes = new TreeMap<>(new ByteArrayComparator());
		File directory = new File(dir);
		if (!directory.exists()) {
			Log.getLog().publish("Directory " + dir + " doesn't exist", this.getClass());
			error.setValue(true);
		}
		else if (!directory.isDirectory()) {
			Log.getLog().publish(dir  + " is not a directory", this.getClass());
			error.setValue(true);
		} else {
			try {
				Files.walk(directory.toPath())
				.filter(p -> !p.getFileName().startsWith("."))
				.forEach(f -> {
					if (f.toAbsolutePath().toString().endsWith(".java")) {
						String filePath = f.toAbsolutePath().toString();
						byte[] hash = JustCodeDigest.digest(f.toFile(), true);
						if (hash == null) {
							Log.getLog().publish("Failed to get hash for " + filePath, JustCodeDigest.getLastException(), this.getClass());
							error.setValue(true);
						}
						else {
							//Log.getLog().publish("Hash for " + filePath + " is " + Arrays.toString(hash), this.getClass());
							String repeatedFile = checkDuplicateHash(hash, hashes);
							if (repeatedFile != null) {
								Log.getLog().publish("Duplicate files found", new Exception("Same hashes for files " + filePath + " and " + repeatedFile), this.getClass());
								error.setValue(true);
							} else {
								hashes.put(hash, filePath);
							}
						}
					}
				});
			} catch (IOException e) {
				Log.getLog().publish("Exception while collecting hashes form " + dir, e, this.getClass());
				error.setValue(true);
			}
		}
		Log.getLog().decLevel();
		if (error.booleanValue()) return null;
		return hashes;
	}
	
	private Map<byte[], String> collectLastGenerationJavaFileHashesInDir(String dir, int generation) {
		String genDir = "???";
		return collectJavaFileHashesInDir(genDir);
	}
	
	private String checkDuplicateHash(byte[] hash, Map<byte[], String> hashes) {
		String repeatedFile = null;
		if (hashes.containsKey(hash)) {
			repeatedFile = hashes.get(hash);
		}
		return repeatedFile;
	}
	
	private boolean checkGeneratedMutants(Map<byte[], String> hashesApiOld, Map<byte[], String> hashesApiNew) {
		Log.getLog().incLevel();
		int mutsOld = hashesApiOld.size();
		int mutsNew = hashesApiNew.size();
		MutableBoolean result = new MutableBoolean(true);
		if (mutsOld == mutsNew) {
			Iterator<Entry<byte[], String>> hashes1It = hashesApiOld.entrySet().iterator();
			while (hashes1It.hasNext()) {
				if (!findCurrentAndRemove(hashes1It, hashesApiNew, true)) {
					Log.getLog().publish("Mutant not found", this.getClass());
					result.setValue(false);
				}
			}
			Iterator<Entry<byte[], String>> hashes2It = hashesApiNew.entrySet().iterator();
			while (hashes2It.hasNext()) {
				if (!findCurrentAndRemove(hashes2It, hashesApiOld, false)) {
					Log.getLog().publish("Mutant not found", this.getClass());
					result.setValue(false);
				}
			}
		} else {
			result.setValue(false);
		}
		Log.getLog().publish("New API generated " + mutsNew + " mutants, and old API generated "+ mutsOld, this.getClass());
		Log.getLog().decLevel();
		return result.booleanValue();
	}
	
	private boolean findCurrentAndRemove(Iterator<Entry<byte[], String>> it, Map<byte[], String> hashes, boolean oldVsNew) {
		Log.getLog().incLevel();
		if (it.hasNext()) {
			Entry<byte[], String> current = it.next();
			if (!hashes.containsKey(current.getKey())) {
				String fromToMsg = oldVsNew?("from old API was not found in mutants from the new API"):("from new API was not found in mutants from the old API");
				Log.getLog().publish("Mutant " + current.getValue() + " " + fromToMsg, this.getClass());
				Log.getLog().decLevel();
				return false;
			} else {
				it.remove();
				hashes.remove(current.getKey());
				Log.getLog().decLevel();
				return true;
			}
		} else {
			Log.getLog().decLevel();
			return true;
		}
	}
	
	private class ByteArrayComparator implements Comparator<byte[]> {
		
		@Override
		public int compare(byte[] v1, byte[] v2) {
			String v1String = Arrays.toString(v1);
			String v2String = Arrays.toString(v2);
			return v1String.compareTo(v2String);
		}
		
	}

}
