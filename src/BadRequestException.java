public class BadRequestException extends Exception{
	private final String ERR_BAD_REQUEST = "400 Bad Request";
	
	@Override
	public String getMessage(){
		return ERR_BAD_REQUEST;
	}
	public BadRequestException() {
		super();
	}
}
