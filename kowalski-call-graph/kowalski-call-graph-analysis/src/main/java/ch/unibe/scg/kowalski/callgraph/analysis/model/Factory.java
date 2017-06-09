package ch.unibe.scg.kowalski.callgraph.analysis.model;

import ch.unibe.scg.kowalski.callgraph.analysis.utility.Cache;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;

public class Factory {

	private Cache<SootClass, Class> classCache;
	private Cache<SootMethod, Method> methodCache;

	public Factory() {
		this.classCache = new Cache<>();
		this.methodCache = new Cache<>();
	}

	public synchronized Invocation toInvocation(Stmt stmt, SootMethod method) {
		return new Invocation(this.toMethod(method), stmt.getJavaSourceStartLineNumber(), stmt.toString());
	}

	public synchronized Method toMethod(SootMethod method) {
		return this.methodCache.getOrPut(method, () -> {
			Class clazz = this.toClass(method.getDeclaringClass());
			String signature = method.getSubSignature();
			return new Method(clazz, signature);
		});
	}

	private Class toClass(SootClass clazz) {
		return this.classCache.getOrPut(clazz, () -> {
			String name = clazz.getName();
			return new Class(name);
		});
	}

}
