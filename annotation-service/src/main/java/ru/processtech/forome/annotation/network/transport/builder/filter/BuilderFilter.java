package ru.processtech.forome.annotation.network.transport.builder.filter;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.util.EnumSet;

public class BuilderFilter {

	public final Class<? extends Filter> filterClass;
	public final String pathSpec;
	public final EnumSet<DispatcherType> dispatches;

	public BuilderFilter(Class<? extends Filter> filterClass, String pathSpec) {
		this(filterClass, pathSpec, EnumSet.of(DispatcherType.REQUEST));
	}

	public BuilderFilter(Class<? extends Filter> filterClass, String pathSpec, EnumSet<DispatcherType> dispatches) {
		this.filterClass = filterClass;
		this.pathSpec = pathSpec;
		this.dispatches = dispatches;
	}
}
