package be.ordina.msdashboard.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

public class ObjectToJsonConverter<T> implements Converter<T, String> {
	private static final Logger LOG = LoggerFactory.getLogger(ObjectToJsonConverter.class);
	private static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writer();

	@Override
	public String convert(T source) {
		if (source == null)
			return null;

		try {
			return OBJECT_WRITER.writeValueAsString(source);
		} catch (JsonProcessingException e) {
			LOG.error("unable to create string value", e);
			throw new IllegalArgumentException("", e);

		}
	}
}
