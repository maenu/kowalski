package ch.unibe.scg.kowalski.callgraph.batch.analysis;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.util.HashMap;

import ch.unibe.scg.kowalski.callgraph.analysis.utility.Cache;

public final class FileSystemSynchronizer {

	public static final FileSystemSynchronizer INSTANCE = new FileSystemSynchronizer();

	private Cache<FileSystem, Integer> semaphores;

	private FileSystemSynchronizer() {
		this.semaphores = new Cache<>();
	}

	public synchronized FileSystem up(URI uri) throws IOException {
		FileSystem fileSystem = this.getOrNewFileSystem(uri);
		Integer semaphore = this.semaphores.getOrPut(fileSystem, () -> {
			return 0;
		});
		this.semaphores.put(fileSystem, semaphore + 1);
		return fileSystem;
	}

	public synchronized void down(FileSystem fileSystem) throws IOException {
		if (!this.semaphores.containsKey(fileSystem)) {
			throw new IllegalArgumentException("No semaphore found for file system. Did you up it?");
		}
		Integer semaphore = this.semaphores.get(fileSystem);
		if (semaphore > 1) {
			this.semaphores.put(fileSystem, semaphore - 1);
		} else {
			this.semaphores.remove(fileSystem);
			fileSystem.close();
		}
	}

	private FileSystem getOrNewFileSystem(URI uri) throws IOException {
		try {
			return FileSystems.getFileSystem(uri);
		} catch (FileSystemNotFoundException exception) {
			return FileSystems.newFileSystem(uri, new HashMap<>());
		}
	}

}
