/**
 * Add supported files here
 * @author Yafim Vodkov 308973882  Nir Tahan 305181166
 *
 */
public enum SupportedFiles {

	/** Files */
	html{
		@Override	
		public String getContentType(){return "text/html";}
	},
	txt{
		@Override
		public String getContentType(){return "text/txt";}
	},
	/** Image files supported */
	bmp{
		@Override	
		public String getContentType(){return "image/bmp";}
	},
	gif{
		@Override	
		public String getContentType(){return "image/gif";}
	},
	png{
		@Override	
		public String getContentType(){return "image/png";}
	},
	jpg{
		@Override	
		public String getContentType(){return "image/jpeg";}
	},

	/** Favicon*/
	ico{
		@Override
		public String getContentType(){return "icon";}
	};



	public abstract String getContentType();

}
