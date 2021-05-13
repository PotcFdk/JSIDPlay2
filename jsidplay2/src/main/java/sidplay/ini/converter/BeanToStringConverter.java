package sidplay.ini.converter;

import static java.beans.Introspector.getBeanInfo;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class BeanToStringConverter {

	private BeanToStringConverter() {
	}

	public static String toString(Object bean) {
		return defaultIfThrows(b -> asList(getBeanInfo(b.getClass(), Object.class).getPropertyDescriptors()).stream()
				.filter(descriptor -> nonNull(descriptor.getReadMethod()))
				.map(defaultIfThrows(
						descriptor -> new SimpleImmutableEntry<>(descriptor.getName(),
								descriptor.getReadMethod().invoke(b)),
						descriptor -> new SimpleImmutableEntry<>(descriptor.getName(),
								"<bean value could not be determined>")))
				.map(SimpleImmutableEntry::toString).collect(Collectors.joining("\n")),
				b -> "<bean introspection failed>").apply(bean);
	}

	private static <T, R> Function<T, R> defaultIfThrows(FunctionThatThrows<T, R> delegate,
			Function<T, R> defaultValueSupplier) {
		return x -> {
			try {
				return delegate.apply(x);
			} catch (Throwable throwable) {
				return defaultValueSupplier.apply(x);
			}
		};
	}

	@FunctionalInterface
	private interface FunctionThatThrows<T, R> {
		R apply(T t) throws Throwable;
	}
}
