package com.cellpose.backend;

import ij.IJ;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.net.JarURLConnection;
import java.nio.charset.StandardCharsets;

public class BackendManager {
    private static final String RESOURCE_ROOT = "backend";
    private static final String EXTRACT_MARKER = ".extracted";

    private Process process;
    private String backendUrl;
    private Path backendDir;

    public synchronized String start() throws IOException {
        if (process != null && process.isAlive()) {
            return backendUrl;
        }

        backendDir = ensureBackendExtracted();
        Path python = findPythonExecutable(backendDir);
        if (python == null) {
            throw new IOException("Bundled Python runtime not found in backend folder.");
        }

        Path startScript = backendDir.resolve("start_backend.py");
        if (!Files.exists(startScript)) {
            throw new IOException("Missing start_backend.py in backend folder.");
        }

        int port = findFreePort();
        backendUrl = "http://127.0.0.1:" + port;

        ProcessBuilder pb = new ProcessBuilder(
            python.toString(),
            startScript.toString(),
            "--host", "127.0.0.1",
            "--port", String.valueOf(port)
        );
        pb.directory(backendDir.toFile());
        pb.redirectErrorStream(true);
        pb.environment().put("PYTHONUNBUFFERED", "1");
        process = pb.start();

        startLogReader(process);
        waitForBackendReady(backendUrl, 30_000);

        return backendUrl;
    }

    public synchronized void stop() {
        if (process == null) return;
        try {
            process.destroy();
            if (process.isAlive()) {
                try {
                    process.waitFor(3, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            process = null;
        }
    }

    private void startLogReader(Process process) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    IJ.log("[Cellpose Backend] " + line);
                }
            } catch (IOException ignored) {
            }
        }, "cellpose-backend-logger");
        t.setDaemon(true);
        t.start();
    }

    private void waitForBackendReady(String url, long timeoutMs) throws IOException {
        long start = System.currentTimeMillis();
        IOException lastError = null;
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url + "/getModels").openConnection();
                conn.setConnectTimeout(1_500);
                conn.setReadTimeout(1_500);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    return;
                }
            } catch (IOException e) {
                lastError = e;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IOException("Backend did not become ready in time.", lastError);
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private Path ensureBackendExtracted() throws IOException {
        Path targetDir = Paths.get(System.getProperty("user.home"), ".cellpose", "backend");
        Path marker = targetDir.resolve(EXTRACT_MARKER);

        if (Files.exists(marker)) {
            return targetDir;
        }

        Files.createDirectories(targetDir);
        extractResourceDirectory(RESOURCE_ROOT, targetDir);
        fixVenvPaths(targetDir);
        Files.write(marker, "ok".getBytes(StandardCharsets.UTF_8));
        return targetDir;
    }

    private void fixVenvPaths(Path backendDir) {
        // Fix pyvenv.cfg files to point to the local py_standalone instead of build paths
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String pyHome;
        if (isWindows) {
            pyHome = backendDir.resolve("py_standalone").resolve("python").toString();
        } else {
            pyHome = backendDir.resolve("py_standalone").resolve("python").toString();
        }

        for (String venvName : new String[]{"venv_v3", "venv_v4"}) {
            Path cfgPath = backendDir.resolve(venvName).resolve("pyvenv.cfg");
            if (Files.exists(cfgPath)) {
                try {
                    java.util.List<String> lines = Files.readAllLines(cfgPath, StandardCharsets.UTF_8);
                    java.util.List<String> newLines = new java.util.ArrayList<>();
                    for (String line : lines) {
                        if (line.startsWith("home = ") || line.startsWith("home=")) {
                            newLines.add("home = " + pyHome);
                        } else {
                            newLines.add(line);
                        }
                    }
                    Files.write(cfgPath, newLines, StandardCharsets.UTF_8);
                    IJ.log("Fixed pyvenv.cfg for " + venvName);
                } catch (IOException e) {
                    IJ.log("Warning: Could not fix pyvenv.cfg for " + venvName + ": " + e.getMessage());
                }
            }

            // On Unix, also fix shebang in bin/python3 script
            if (!isWindows) {
                fixUnixPythonScript(backendDir, venvName, pyHome);
            }
        }
    }

    private void fixUnixPythonScript(Path backendDir, String venvName, String pyHome) {
        Path binDir = backendDir.resolve(venvName).resolve("bin");
        String[] scripts = {"python", "python3", "pip", "pip3"};
        String newShebang = "#!" + pyHome + "/bin/python3";

        for (String script : scripts) {
            Path scriptPath = binDir.resolve(script);
            if (Files.exists(scriptPath) && !Files.isSymbolicLink(scriptPath)) {
                try {
                    java.util.List<String> lines = Files.readAllLines(scriptPath, StandardCharsets.UTF_8);
                    if (!lines.isEmpty() && lines.get(0).startsWith("#!")) {
                        lines.set(0, newShebang);
                        Files.write(scriptPath, lines, StandardCharsets.UTF_8);
                        // Ensure executable
                        scriptPath.toFile().setExecutable(true, false);
                        IJ.log("Fixed shebang in " + venvName + "/bin/" + script);
                    }
                } catch (IOException e) {
                    IJ.log("Warning: Could not fix shebang for " + venvName + "/bin/" + script + ": " + e.getMessage());
                }
            }
        }
    }

    private void extractResourceDirectory(String resourceRoot, Path targetDir) throws IOException {
        URL resourceUrl = getClass().getClassLoader().getResource(resourceRoot);
        if (resourceUrl == null) {
            throw new IOException("Backend resources not found: " + resourceRoot);
        }

        if ("jar".equalsIgnoreCase(resourceUrl.getProtocol())) {
            JarURLConnection jarConnection = (JarURLConnection) resourceUrl.openConnection();
            try (JarFile jarFile = jarConnection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(resourceRoot + "/")) {
                        continue;
                    }
                    String relative = name.substring(resourceRoot.length() + 1);
                    if (relative.isEmpty()) {
                        continue;
                    }
                    Path destPath = targetDir.resolve(relative);
                    if (entry.isDirectory()) {
                        Files.createDirectories(destPath);
                    } else {
                        Files.createDirectories(destPath.getParent());
                        Files.copy(jarFile.getInputStream(entry), destPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } else if ("file".equalsIgnoreCase(resourceUrl.getProtocol())) {
            try {
                Path sourcePath = Paths.get(resourceUrl.toURI());
                copyDirectory(sourcePath, targetDir);
            } catch (Exception e) {
                throw new IOException("Failed to resolve backend resource path.", e);
            }
        } else {
            throw new IOException("Unsupported resource protocol: " + resourceUrl.getProtocol());
        }
    }

    private void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(dir);
                Path target = targetDir.resolve(relative.toString());
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(file);
                Path target = targetDir.resolve(relative.toString());
                Files.createDirectories(target.getParent());
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private Path findPythonExecutable(Path baseDir) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String binDir = isWindows ? "Scripts" : "bin";
        String exe = isWindows ? "python.exe" : "python3";

        // Check venv_v4 first (CellposeSAM) - this has the FastAPI server deps
        Path v4 = baseDir.resolve("venv_v4").resolve(binDir).resolve(exe);
        if (Files.exists(v4)) return v4;
        // Try python.exe as fallback on Windows
        if (isWindows) {
            v4 = baseDir.resolve("venv_v4").resolve(binDir).resolve("python.exe");
            if (Files.exists(v4)) return v4;
        }

        // Check venv_v3 (Cellpose 3.1)
        Path v3 = baseDir.resolve("venv_v3").resolve(binDir).resolve(exe);
        if (Files.exists(v3)) return v3;
        if (isWindows) {
            v3 = baseDir.resolve("venv_v3").resolve(binDir).resolve("python.exe");
            if (Files.exists(v3)) return v3;
        }

        // Check standalone Python as last resort
        if (isWindows) {
            Path standalone = baseDir.resolve("py_standalone").resolve("python").resolve("python.exe");
            if (Files.exists(standalone)) return standalone;
        } else {
            Path standalone = baseDir.resolve("py_standalone").resolve("python").resolve("bin").resolve("python3");
            if (Files.exists(standalone)) return standalone;
        }

        return null;
    }
}
