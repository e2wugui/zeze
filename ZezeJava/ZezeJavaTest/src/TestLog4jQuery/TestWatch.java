package TestLog4jQuery;

import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.*;
public class TestWatch {
	public static void main(String [] args) throws Exception {
		var watchService = FileSystems.getDefault().newWatchService();
		var directory = Paths.get(".");
		directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
		while (true) {
			var key = watchService.take();
			for (var event : key.pollEvents()) {
				var kind = event.kind();
				if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
					@SuppressWarnings("unchecked") WatchEvent<Path> renameEvent = (WatchEvent<Path>) event;
					Path renamedFile = renameEvent.context();
					System.out.println("Created: " + renamedFile);
				}
			}
			if (!key.reset())
				break;
		}
	}
}
