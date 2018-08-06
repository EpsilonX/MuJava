package mujava.junit.runner;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runners.Parameterized;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.RunnerBuilder;

//TODO: comment
public class FailFastCapableRunnerBuilder extends RunnerBuilder {
	
	private boolean failFast;
	private long timeout;
	private boolean verbose = MuJavaJunitTestRunnerBuilder.verbose;
	
	public FailFastCapableRunnerBuilder(boolean failFast, long timeout) {
		this.failFast = failFast;
		this.timeout = timeout;
	}

	@Override
	public Runner runnerForClass(Class<?> arg0) throws Throwable {
		return retrieveTestRunner(arg0);
	}
	
	private ParentRunner<?> retrieveTestRunner(Class<?> testToRun) throws Throwable {
		if (verbose) System.out.println("mujava.junit.runner.FailFastCapableRunnerBuilder: retrieving runner for class " + testToRun.getName());
		RunWith runWithAnnotation = testToRun.getAnnotation(RunWith.class);
		if (runWithAnnotation != null) {
			if (verbose) System.out.println("runWith annotation found");
			if (runWithAnnotation.value().equals(Parameterized.class)) {
				if (verbose) System.out.println("runWith Parameterized.class");
				return new FailFastCapableParameterized(testToRun, failFast, timeout);
			} else if (runWithAnnotation.value().equals(Suite.class)) {
				if (verbose) System.out.println("runWith Suite.class");
				return new Suite(testToRun, this);
			}
		} else if (TestCase.class.isAssignableFrom(testToRun)) {
			if (verbose) System.out.println("Test class extends TestCase");
			FailFastCapableBlockJUnit4ClassRunner.ignore = false;
			return new FailFastCapableBlockJUnit4ClassRunner(testToRun, failFast, timeout);
		} else if (TestSuite.class.isAssignableFrom(testToRun)) {
			if (verbose) System.out.println("Test class extends TestSuite");
			FailFastCapableBlockJUnit4ClassRunner.ignore = false;
			return new FailFastCapableBlockJUnit4ClassRunner(testToRun, failFast, timeout);
		} else if (testToRun.getAnnotation(Test.class) != null) {
			if (verbose) System.out.println("Test annotation found in test class");
			FailFastCapableBlockJUnit4ClassRunner.ignore = false;
			return new FailFastCapableBlockJUnit4ClassRunner(testToRun, failFast, timeout);
		} else {
			boolean hasAtLeastOneTest = false;
			for (Method m : testToRun.getDeclaredMethods()) {
				if (m.isAnnotationPresent(Test.class)) {
					hasAtLeastOneTest = true;
					break;
				}
			}
			if (hasAtLeastOneTest) {
				if (verbose) System.out.println("At least one test method found");
				FailFastCapableBlockJUnit4ClassRunner.ignore = false;
				return new FailFastCapableBlockJUnit4ClassRunner(testToRun, failFast, timeout);
			}
			throw new IllegalArgumentException("Class : " + testToRun.toString() + " is not a valid junit test");
		}
		if (verbose) System.out.println("No conditions met");
		FailFastCapableBlockJUnit4ClassRunner.ignore = false;
		return new FailFastCapableBlockJUnit4ClassRunner(testToRun, false, timeout);
	}

}
