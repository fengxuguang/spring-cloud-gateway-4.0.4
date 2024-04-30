/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gateway.route;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;

import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author Spencer Gibb
 */
public class CachingRouteDefinitionLocator implements RouteDefinitionLocator, ApplicationListener<RefreshRoutesEvent> {

	private static final String CACHE_KEY = "routeDefs";

	// 路由定义定位器
	private final RouteDefinitionLocator delegate;

	// 路由定义信息
	private final Flux<RouteDefinition> routeDefinitions;

	// 本地缓存集合
	private final Map<String, List> cache = new ConcurrentHashMap<>();

	public CachingRouteDefinitionLocator(RouteDefinitionLocator delegate) {
		this.delegate = delegate;
		routeDefinitions = CacheFlux.lookup(cache, CACHE_KEY, RouteDefinition.class).onCacheMissResume(this::fetch);
	}

	private Flux<RouteDefinition> fetch() {
		return this.delegate.getRouteDefinitions();
	}

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		return this.routeDefinitions;
	}

	/**
	 * Clears the cache of routeDefinitions.
	 * @return routeDefinitions flux
	 */
	// 刷新本地缓存, 先清空本地缓存再获取一份新的路由定义信息存储
	public Flux<RouteDefinition> refresh() {
		this.cache.clear();
		return this.routeDefinitions;
	}

	@Override
	public void onApplicationEvent(RefreshRoutesEvent event) {
		fetch().materialize().collect(Collectors.toList()).doOnNext(routes -> cache.put(CACHE_KEY, routes)).subscribe();
	}

}
