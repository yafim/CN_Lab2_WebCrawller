import java.util.ArrayList;


public class ExtensionsChecker {
	private ArrayList<String> m_ImagesExtensions;
	private ArrayList<String> m_VideosExtensions;
	private ArrayList<String> m_DocumentsExtensions;

	public ExtensionsChecker(ArrayList<String> imagesExtensions, ArrayList<String> videosExtensions, ArrayList<String> documentsExtensions) {
		this.m_ImagesExtensions = imagesExtensions;
		this.m_VideosExtensions = videosExtensions;
		this.m_DocumentsExtensions = documentsExtensions;
	}
	
	public boolean isImageExtension(String extension) {
		return m_ImagesExtensions.contains(extension);
	}
	
	public boolean isVideoExtension(String extension) {
		return m_VideosExtensions.contains(extension);
	}
	
	public boolean isDocumentsExtension(String extension) {
		return m_DocumentsExtensions.contains(extension);
	}
}
