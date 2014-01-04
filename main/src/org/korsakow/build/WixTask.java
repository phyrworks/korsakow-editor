package org.korsakow.build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.korsakow.ide.io.AsyncStreamPipe;
import org.korsakow.ide.util.FileUtil;
import org.korsakow.ide.util.Util;


/**
 * Ant wrapper for WIX. Runs candle and light to generate a windows installer.
 */
public class WixTask extends Task {
	private String wixHome;
	private String workingDir;
	private String outputFile;
	private final List<FileSet> filesets = new ArrayList<FileSet>();
	
	/**
	 * Specifies the wix installation directory. Not required if the WIX environment variable
	 * is set (which is done by the wix installer) or you wish to override it.
	 */
	public void setWixHome(String wixHome) {
		this.wixHome = wixHome;
	}
	/**
	 * Optional directory where intermediate files will be generated. A tempdir is
	 * used otherwise.
	 */
	public void setWorkingDir(String workingDir) {
		this.workingDir = workingDir;
	}
	/**
	 * The name of the msi/exe to generate.
	 */
	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}
	
	/**
	 * Support for ant filesets
 	 */
    public void addFileset(FileSet fileset) {
        filesets.add(fileset);
    }	
    
	@Override
	public void execute() throws BuildException {
		try {
			if (wixHome == null || wixHome.trim().isEmpty())
				wixHome = System.getenv("WIX");
			if (wixHome == null || wixHome.trim().isEmpty()
				|| !new File(wixHome).isDirectory())
				throw new IllegalArgumentException("Could not find Wix, tried: " + wixHome);
			
			if (workingDir == null)
				workingDir = FileUtil.createTempDirectory("wixtask", "work").getPath();
			if (outputFile == null || outputFile.trim().isEmpty())
				throw new IllegalArgumentException("Missing or empty outputPath");
			
			
			log(String.format("WixHome '%s'", wixHome));
			log(String.format("Working dir '%s'", workingDir));
			log(String.format("Output path '%s'", outputFile));

			exec();
		} catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
	}
	private void exec() throws Exception {
		File binDir = new File(wixHome, "bin");
		String candleExe = new File(binDir, "candle.exe").getCanonicalPath();
		String lightExe = new File(binDir, "light.exe").getCanonicalPath();
		
		List<String> candleFiles = new ArrayList<String>();
		candleFiles.add(candleExe);
		for (FileSet fs : filesets) {
			File dir = fs.getDir(getProject());
			for (String included : fs.getDirectoryScanner(getProject()).getIncludedFiles()) {
				candleFiles.add(new File(dir, included).getPath());
			}
		}
		log(String.format("Compiling with candle: '%s'", Util.join(candleFiles)));
		cmdExec(candleFiles);

		List<String> lightFiles = new ArrayList<String>();
		lightFiles.addAll(Arrays.asList(
			lightExe,
			"-ext", "WixUIExtension",
			"-pdbout", FileUtil.setFileExtension(new File(workingDir, new File(outputFile).getName()).getPath(), "wixpdb"),
			"-out", outputFile
		));
		for (FileSet fs : filesets) {
			for (String included : fs.getDirectoryScanner(getProject()).getIncludedFiles()) {
				lightFiles.add(FileUtil.setFileExtension(new File(workingDir, included).getPath(), "wixobj"));
			}
		}
		log(String.format("Linking with light: '%s'", Util.join(lightFiles)));
		cmdExec(lightFiles);
	}
	
	private void cmdExec(List<String> args) throws InterruptedException, IOException {
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.directory(new File(workingDir));
		Process proc = pb.start();
		ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
		ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
		AsyncStreamPipe<InputStream, OutputStream> stdOutPipe = new AsyncStreamPipe<InputStream, OutputStream>(proc.getInputStream(), stdOut);
		AsyncStreamPipe<InputStream, OutputStream> stdErrPipe = new AsyncStreamPipe<InputStream, OutputStream>(proc.getErrorStream(), stdErr);
		stdOutPipe.start();
		stdErrPipe.start();
		try {
			int exitCode = proc.waitFor();
			if (exitCode != 0) {
				StringBuilder sb = new StringBuilder();
				sb.append("Error executing ").append(args).append('\n');
				sb.append("Std out:").append('\n').append(stdOut.toString()).append('\n');
				sb.append("Std err:").append('\n').append(stdErr.toString()).append('\n');
				throw new IllegalArgumentException(sb.toString());
			}
		} finally {
			if (stdOutPipe != null) try { stdOutPipe.join(); } catch (Exception e) {}
			if (stdErrPipe != null) try { stdErrPipe.join(); } catch (Exception e) {}
		}
	}
}