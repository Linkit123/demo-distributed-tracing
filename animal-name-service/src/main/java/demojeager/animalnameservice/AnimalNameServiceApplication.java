package demojeager.animalnameservice;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@SpringBootApplication
public class AnimalNameServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnimalNameServiceApplication.class, args);
	}

	@Bean
	public Tracer tracer() {
		Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
				.withType(ConstSampler.TYPE)
				.withParam(1);

		Configuration.ReporterConfiguration reporterConfig = Configuration.ReporterConfiguration.fromEnv()
				.withLogSpans(true);

		Configuration config = new Configuration("name-svc")
				.withSampler(samplerConfig)
				.withReporter(reporterConfig);

		return config.getTracer();
	}
}

@RestController
@RequestMapping("/api/v1/animals")
class AnimalNameResource {

	private final List<String> animalNames;
	private final Random random;
	private final Tracer tracer;

	public AnimalNameResource(Tracer tracer) throws IOException {
		this.tracer = tracer;
		InputStream inputStream = new ClassPathResource("static/animals.txt").getInputStream();
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
			animalNames = reader.lines().collect(Collectors.toList());
		}
		random = new Random();
	}

	@GetMapping(path = "/random")
	public String name(@RequestHeader HttpHeaders headers) {
		SpanContext parentContext = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers.toSingleValueMap()));
		Span span = tracer.buildSpan("find-random-animal-name").asChildOf(parentContext).start();
		String name = animalNames.get(random.nextInt(animalNames.size()));
		span.finish();
		return name;
	}
}