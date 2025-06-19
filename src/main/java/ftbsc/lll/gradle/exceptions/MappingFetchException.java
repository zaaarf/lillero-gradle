package ftbsc.lll.gradle.exceptions;

public class MappingFetchException extends RuntimeException {
	public MappingFetchException(String provider, Throwable cause) {
		super(String.format("Failed to get %s mappings!", provider), cause);
	}
}
