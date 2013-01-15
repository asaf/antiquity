/**
 * Copyright (c) 2012-2013 "Indexia Technologies, ltd."
 *
 * This file is part of Antiquity.
 *
 * Antiquity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package co.indexia.antiquity.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TestSuite;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.GraphTest;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import co.indexia.antiquity.range.Range;

public class VersionedGraphTestSuite<V extends Comparable<V>> extends TestSuite {
	public VersionedGraphTestSuite() {

	}

	public VersionedGraphTestSuite(final GraphTest graphTest) {
		super(graphTest);
	}

	public void testGraphConfVertexShouldExist() {
		VersionedGraph<TinkerGraph, V> graph = getGraphInstance();
		assertTrue(graph.getVersionConfVertex() != null);
	}

	public void testForVersionVertexProperty() {
		VersionedGraph<TinkerGraph, V> graph = getGraphInstance();
		Vertex fooV = graph.addVertex("fooV");
		V forVer = ((VersionedVertex<V>) fooV).getForVersion();
		assertNotNull(forVer);
		commitIfTransactional(graph);
		// Required by transactional graph as version is stamped only during commit
		forVer = ((VersionedVertex<V>) fooV).getForVersion();
		// in transactional graph, after commit value should be updated with new version
		assertSame(forVer, ((VersionedVertex<V>) fooV).getForVersion());
		// --
		fooV.setProperty("key", "foo");
		commitIfTransactional(graph);
		V forVerAfterCommit = ((VersionedVertex<V>) fooV).getForVersion();
		assertNotSame(forVer, forVerAfterCommit);
	}

	public void testUpdatedVertexInstanceShouldAlwaysContainTheLatestVersionAndData() {
		VersionedGraph<TinkerGraph, V> graph = getGraphInstance();
		VersionedVertex<V> fooV = (VersionedVertex<V>) graph.addVertex("fooV");
		commitIfTransactional(graph);
		assertNull(fooV.getProperty("key"));
		int keysSize = fooV.getPropertyKeys().size();
		assertEquals(graph.getLatestGraphVersion(), fooV.getForVersion());
		assertEquals(keysSize, fooV.getPropertyKeys().size());
		fooV.setProperty("key", "foo");
		assertEquals("foo", fooV.getProperty("key"));
		assertEquals(keysSize + 1, fooV.getPropertyKeys().size());
		commitIfTransactional(graph);
		assertEquals(graph.getLatestGraphVersion(), fooV.getForVersion());
		assertEquals("foo", fooV.getProperty("key"));
		assertEquals(keysSize + 1, fooV.getPropertyKeys().size());
	}

	public void testAddingNewVersionedVertices() {
		VersionedGraph<TinkerGraph, V> graph = getGraphInstance();
		Vertex fooV = graph.addVertex("fooV");
		commitIfTransactional(graph);
		V ver1 = graph.getLatestGraphVersion();
		assertEquals(Range.range(ver1, graph.getMaxPossibleGraphVersion()), graph.getVersionRange(fooV));

		fooV.setProperty("prop", "foo");
		commitIfTransactional(graph);
		V ver2 = graph.getLatestGraphVersion();
		assertEquals(Range.range(ver2, graph.getMaxPossibleGraphVersion()), graph.getVersionRange(fooV));

		// TODO: Check why getGraphNodesString() fails
		// System.out.println(getGraphNodesString());
		Vertex barV = graph.addVertex("barV");
		commitIfTransactional(graph);
		V ver3 = graph.getLatestGraphVersion();
		barV.setProperty("prop", "bar");
		commitIfTransactional(graph);
		V ver4 = graph.getLatestGraphVersion();
		Vertex fooVLoaded = graph.getVertex(fooV.getId());
		Vertex barVLoaded = graph.getVertex(barV.getId());
		assertEquals(fooV, fooVLoaded);
		assertEquals(barV, barVLoaded);

		assertEquals(Range.range(ver2, graph.getMaxPossibleGraphVersion()), graph.getVersionRange(fooVLoaded));
		assertEquals(Range.range(ver4, graph.getMaxPossibleGraphVersion()), graph.getVersionRange(barVLoaded));
	}

	public void testAddingNewVersionedEdges() {
		VersionedGraph<TinkerGraph, V> graph = getGraphInstance();
		Vertex fooV = graph.addVertex("fooV");
		commitIfTransactional(graph);
		Vertex barV = graph.addVertex("barV");
		commitIfTransactional(graph);
		Edge e = graph.addEdge("fooBarE", fooV, barV, "LINKED");
		commitIfTransactional(graph);

		assertEquals(e, graph.getEdge(e.getId()));
		assertNotNull(graph.getEdge(e.getId()));
	}

	public void testRemovingVersionedVertices() {
		VersionedGraph<TinkerGraph, V> graph = getGraphInstance();
		Vertex fooV = graph.addVertex("fooV");
		commitIfTransactional(graph);
		V fooVer = graph.getLatestGraphVersion();
		Vertex barV = graph.addVertex("barV");
		commitIfTransactional(graph);
		V barVer = graph.getLatestGraphVersion();
		graph.removeVertex(fooV);
		commitIfTransactional(graph);
		V barPostFooRemovalVer = graph.getLatestGraphVersion();

		assertEquals(Range.range(fooVer, barVer), graph.getVersionRange(graph.getVertex(fooV.getId())));
		assertEquals(Range.range(barVer, graph.getMaxPossibleGraphVersion()),
				graph.getVersionRange(graph.getVertex(barV.getId())));

		graph.removeVertex(barV);
		commitIfTransactional(graph);
		assertEquals(Range.range(barVer, barPostFooRemovalVer), graph.getVersionRange(graph.getVertex(barV.getId())));
	}

	public void testVersionedVertexProperties() {
		VersionedGraph<TinkerGraph, V> graph = getGraphInstance();
		VersionedVertex<V> v = (VersionedVertex<V>) graph.addVertex("fooV");
		commitIfTransactional(graph);
		int propsSize = v.getPropertyKeys().size();
		V emptyVersion = graph.getLatestGraphVersion();
		v.setProperty("prop", "foo");
		commitIfTransactional(graph);
		// TODO: Consider automated latest version set to a VersionedVertex when setting properties.
		v.setForVersion(graph.getLatestGraphVersion());
		assertEquals(propsSize + 1, v.getPropertyKeys().size());
		V fooVersion = graph.getLatestGraphVersion();
		v.setProperty("prop", "bar");
		commitIfTransactional(graph);
		// TODO: Consider automated latest version set to a VersionedVertex when setting properties.
		v.setForVersion(graph.getLatestGraphVersion());
		assertEquals(propsSize + +1, v.getPropertyKeys().size());
		V barVersion = graph.getLatestGraphVersion();
		v.setProperty("prop", "baz");
		commitIfTransactional(graph);
		V bazVersion = graph.getLatestGraphVersion();
		v.setProperty("prop", "qux");
		commitIfTransactional(graph);
		V quxVersion = graph.getLatestGraphVersion();
		v.setProperty("newProp", "fooNew");
		commitIfTransactional(graph);
		// TODO: Consider automated latest version set to a VersionedVertex when setting properties.
		v.setForVersion(graph.getLatestGraphVersion());
		assertEquals(propsSize + +2, v.getPropertyKeys().size());

		assertEquals("foo", graph.getVertexForVersion(v, fooVersion).getProperty("prop"));
		assertEquals("bar", graph.getVertexForVersion(v, barVersion).getProperty("prop"));
		assertEquals("baz", graph.getVertexForVersion(v, bazVersion).getProperty("prop"));
		assertEquals("qux", graph.getVertexForVersion(v, quxVersion).getProperty("prop"));

		ArrayList<Vertex> chain = new ArrayList<Vertex>();
		VersionedGraph.getVertexChain(chain, v);
		assertEquals(6, chain.size());

		// Compare via getId() and not via EventElement.equals coz class types might be different
		// (HistoricalVertex/VersionedVertex)
		assertEquals(chain.get(0), v);
		assertEquals(chain.get(2).getId(), graph.getVertexForVersion(v, bazVersion).getId());
		assertEquals(chain.get(3).getId(), graph.getVertexForVersion(v, barVersion).getId());
		assertEquals(chain.get(4).getId(), graph.getVertexForVersion(v, fooVersion).getId());
		assertEquals(chain.get(5).getId(), graph.getVertexForVersion(v, emptyVersion).getId());
	}

	public void testVersionedVertexEdges() {
		VersionedGraph<TinkerGraph, V> graph = getGraphInstance();
		Vertex vertex1 = graph.addVertex("vertex1");
		commitIfTransactional(graph);
		Vertex vertex2 = graph.addVertex("vertex2");
		commitIfTransactional(graph);
		V verAfterVerticesCreation = graph.getLatestGraphVersion();
		vertex1.setProperty("key1", "foo");
		commitIfTransactional(graph);
		vertex2.setProperty("key2", "foo");
		commitIfTransactional(graph);
		V verBeforeEdges = graph.getLatestGraphVersion();
		Edge e1 = graph.addEdge("v1v2_1", vertex1, vertex2, "V1_TO_V2");
		commitIfTransactional(graph);
		V verAfterEdge1 = graph.getLatestGraphVersion();
		Edge e2 = graph.addEdge("v1v2_2", vertex1, vertex2, "V1_TO_V2");
		commitIfTransactional(graph);
		V verAfterEdge2 = graph.getLatestGraphVersion();
		graph.removeEdge(e1);
		commitIfTransactional(graph);
		V verAfterDelOfE1 = graph.getLatestGraphVersion();
		graph.removeEdge(e2);
		commitIfTransactional(graph);
		V verAfterDelOfBothEdges = graph.getLatestGraphVersion();

		Vertex vv1 = graph.getVertexForVersion(vertex1, verAfterVerticesCreation);
		Vertex vv2 = graph.getVertexForVersion(vertex1, verBeforeEdges);
		assertEquals(0, Lists.newArrayList(vv2.getEdges(Direction.OUT, "V1_TO_V2")).size());
		Vertex vv3 = graph.getVertexForVersion(vertex1, verAfterEdge1);
		assertEquals(1, Lists.newArrayList(vv2.getEdges(Direction.OUT, "V1_TO_V2")).size());
		assertEquals(e1.getId(), Lists.newArrayList(vv2.getEdges(Direction.OUT, "V1_TO_V2")).get(0).getId());
		Vertex vv4 = graph.getVertexForVersion(vertex1, verAfterEdge2);

		// TODO: Compare via ID until Element.equals is overriden and ignores class type comparison
		Set<Object> realIds = new HashSet<Object>();
		realIds.add(e1.getId());
		realIds.add(e2.getId());
		List<Edge> loadedEdges = Lists.newArrayList(vv2.getEdges(Direction.OUT, "V1_TO_V2"));
		Set<Object> loadedIds = new HashSet<Object>();
		for (Edge e : loadedEdges)
			loadedIds.add(e.getId());
		assertEquals(realIds, loadedIds);

		Vertex vv5 = graph.getVertexForVersion(vertex1, verAfterDelOfE1);
		assertEquals(1, Lists.newArrayList(vv5.getEdges(Direction.OUT, "V1_TO_V2")).size());
		assertEquals(e2.getId(), Lists.newArrayList(vv5.getEdges(Direction.OUT, "V1_TO_V2")).get(0).getId());

		Vertex vv6 = graph.getVertexForVersion(vertex1, verAfterDelOfBothEdges);
		assertEquals(0, Lists.newArrayList(vv6.getEdges(Direction.OUT, "V1_TO_V2")).size());
	}

	public void testEmptyVerticesPrivateHash() {
		VersionedGraph<?, V> graph = getGraphInstance();
		VersionedVertex<Long> v1 = (VersionedVertex) graph.addVertex("v1");
		commitIfTransactional(graph);
		assertNotNull(graph.getPrivateHash(v1));
		String hashOfEmptyVer1 = graph.getPrivateHash(v1);
		VersionedVertex<Long> v2 = (VersionedVertex) graph.addVertex("v2");
		commitIfTransactional(graph);
		assertNotNull(v2.getProperty(VersionedGraph.PRIVATE_HASH_PROP_KEY));
		String hashOfEmptyVer2 = graph.getPrivateHash(v2);
		// Two empty vertices should not have equal hash
		assertNotSame(hashOfEmptyVer1, hashOfEmptyVer2);
	}

	public void testVerticesWithPropsPrivateHash() {
		VersionedGraph<TinkerGraph, V> graph = getGraphInstance();
		VersionedVertex<Long> v1 = (VersionedVertex) graph.addVertex("v1");
		commitIfTransactional(graph);
		String v1EmptyHash = graph.getPrivateHash(v1);
		v1.setProperty("keyFoo", "foo");
		commitIfTransactional(graph);
		String v1With1KeyHash = graph.getPrivateHash(v1);
		v1.setProperty("keyBar", "bar");
		commitIfTransactional(graph);
		String v1With2KeysHash = graph.getPrivateHash(v1);
		v1.setProperty("keyBaz", "baz");
		commitIfTransactional(graph);
		String v1With3KeysHash = graph.getPrivateHash(v1);
		commitIfTransactional(graph);
		assertNotSame(v1With1KeyHash, v1With2KeysHash);
		assertNotSame(v1With1KeyHash, v1With3KeysHash);
		assertNotSame(v1With2KeysHash, v1With3KeysHash);

		VersionedVertex<Long> v2 = (VersionedVertex) graph.addVertex("v2");
		commitIfTransactional(graph);
		v2.setProperty("keyFoo", "foo");
		commitIfTransactional(graph);
		v2.setProperty("keyBar", "bar");
		commitIfTransactional(graph);
		v2.setProperty("keyBaz", "baz");
		commitIfTransactional(graph);
		String v2HashWith3Props = graph.getPrivateHash(v2);
		assertNotSame(v1With3KeysHash, v2HashWith3Props);

		String v1BeforeQux = graph.getPrivateHash(v1);
		v1.setProperty("keyQux", "qux");
		commitIfTransactional(graph);
		assertNotSame(v1BeforeQux, graph.getPrivateHash(v1));

		v1.removeProperty("keyQux");
		commitIfTransactional(graph);
		assertEquals(v1BeforeQux, graph.getPrivateHash(v1));
	}

	private String getGraphNodesString() {
		VersionedGraph<TinkerGraph, V> graph = getGraphInstance();
		StringBuffer graphStr = new StringBuffer();
		for (Vertex v : graph.getVertices()) {
			if (v.getId().equals(graph.getVersionConfVertex().getId()))
				continue;
			graphStr.append("Vertex [" + v.getId() + "]");

			for (String key : v.getPropertyKeys()) {
				graphStr.append("\n");
				graphStr.append("\tProp [").append(key).append("] with value [").append(v.getProperty(key)).append("]");
			}

			graphStr.append("\n");
		}

		return graphStr.toString();
	}

	private void printEdges(Iterable<Edge> edges) {
		for (Edge e : edges) {
			System.out.println(e);
		}
	}

	private VersionedGraph<TinkerGraph, V> getGraphInstance() {
		return (VersionedGraph<TinkerGraph, V>) graphTest.generateGraph();
	}

	public void commitIfTransactional(Graph graph) {
		if (graph instanceof TransactionalGraph)
			((TransactionalGraph) graph).commit();
	}
}
