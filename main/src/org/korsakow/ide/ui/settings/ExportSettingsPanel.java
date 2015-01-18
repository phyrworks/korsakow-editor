/**
 * 
 */
package org.korsakow.ide.ui.settings;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.korsakow.ide.Application;
import org.korsakow.ide.ui.factory.IUIFactory;
import org.korsakow.ide.ui.factory.UIFactory;
import org.korsakow.ide.util.FileUtil;

public class ExportSettingsPanel extends JPanel implements ISettingsPanel
{
	private JCheckBox exportVideosCheck;
	private JCheckBox exportSoundsCheck;
	private JCheckBox exportSubtitlesCheck;
	private JCheckBox exportImagesCheck;
	private JCheckBox exportWebFilesCheck;
	private JTextField exportDirectory;
	private JButton exportDirectoryButton;

	public ExportSettingsPanel()
	{
		initUI();
		initListeners();
	}
	private void initUI()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		JPanel panel;
		add(panel = createExportTypesPanel());
		panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		add(panel = createFolderPanel());
		panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		
		add(Box.createVerticalGlue());
	}
	private JPanel createExportTypesPanel() {
		JPanel content = new JPanel();
		content.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		
		content.add(new JLabel("Types to export:"));

		content.add( Box.createVerticalStrut(5) );
		
		Box checkPanel = Box.createVerticalBox();
		checkPanel.setAlignmentX(LEFT_ALIGNMENT);
		checkPanel.setBorder(BorderFactory.createEmptyBorder(0,20,0,0));
		checkPanel.add( exportVideosCheck = new JCheckBox("Videos") );
		checkPanel.add( Box.createVerticalStrut(5) );
		checkPanel.add( exportImagesCheck = new JCheckBox("Images") );
		checkPanel.add( Box.createVerticalStrut(5) );
		checkPanel.add( exportSoundsCheck = new JCheckBox("Sounds") );
		checkPanel.add( Box.createVerticalStrut(5) );
		checkPanel.add( exportSubtitlesCheck = new JCheckBox("Subtitles") );
		checkPanel.add( Box.createVerticalStrut(5) );
		checkPanel.add( exportWebFilesCheck = new JCheckBox("Web Files") );
		content.add(checkPanel);

		content.add( Box.createVerticalStrut(15) );
		
		content.add(new JLabel("Export location"));
		Box exportPanel = Box.createHorizontalBox();
		exportPanel.setAlignmentX(LEFT_ALIGNMENT);
		exportPanel.add(exportDirectory = new JTextField());
		exportDirectory.setMinimumSize(new Dimension(200, 25));
		exportDirectory.setMaximumSize(new Dimension(2000, 25));
		exportPanel.add(exportDirectoryButton = new JButton("..."));
		content.add(exportPanel);
		
		content.add( Box.createVerticalStrut(10) );
		
		return content;
	}
	private JPanel createFolderPanel()
	{
		IUIFactory fac = UIFactory.getFactory();
		JPanel folderPanel = new JPanel();
		return folderPanel;
	}
	private void initListeners()
	{
		exportDirectoryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String currentValue = exportDirectory.getText().trim();
				if (currentValue.isEmpty())
					currentValue = "index.html";
				File file = Application.getInstance().showFileSaveDialog(exportDirectoryButton, new File(currentValue));
				if (file != null) {
					if (FileUtil.isProbablyADirectory(file))
						file = new File(file, "index.html");
					exportDirectory.setText(file.getPath());
				}
			}
		});
		
		class Test implements ActionListener
		{
			public void actionPerformed(ActionEvent event ) {
				final JCheckBox source = (JCheckBox)event.getSource();
				if (!source.isSelected())
					Application.getInstance().showOneTimeAlertDialog("ExportSettings.exportTypes", source, "Reminder", "Please remember to do a full export at least once!");
			}
		}
		exportVideosCheck.addActionListener(new Test());
		exportImagesCheck.addActionListener(new Test());
		exportSoundsCheck.addActionListener(new Test());
		exportSubtitlesCheck.addActionListener(new Test());
		exportWebFilesCheck.addActionListener(new Test());
	}
	public boolean getExportVideos() {
		return exportVideosCheck.isSelected();
	}
	public void setExportVideos(boolean video) {
		exportVideosCheck.setSelected(video);
	}
	public boolean getExportImages() {
		return exportImagesCheck.isSelected();
	}
	public void setExportImages(boolean image) {
		exportImagesCheck.setSelected(image);
	}
	public boolean getExportSounds() {
		return exportSoundsCheck.isSelected();
	}
	public void setExportSounds(boolean sound) {
		exportSoundsCheck.setSelected(sound);
	}
	public boolean getExportSubtitles() {
		return exportSubtitlesCheck.isSelected();
	}
	public void setExportSubtitles(boolean subtitle) {
		exportSubtitlesCheck.setSelected(subtitle);
	}
	public boolean getExportWebFiles() {
		return exportWebFilesCheck.isSelected();
	}
	public void setExportWebFiles(boolean web) {
		exportWebFilesCheck.setSelected(web);
	}
	
	public String getExportDirectory() {
		return exportDirectory.getText();
	}
	public void setExportDirectory(String dir) {
		exportDirectory.setText(dir);
	}
	
	public void dispose()
	{
	}
}
