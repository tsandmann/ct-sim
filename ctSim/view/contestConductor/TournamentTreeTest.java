package ctSim.view.contestConductor;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class TournamentTreeTest {
	private TournamentTree<Integer> singleNode;
	private TournamentTree<Integer> twoNodes;
	private TournamentTree<Integer> twoLowestLevelGames;
	private TournamentTree<Integer> fifteenGreencards;
	private TournamentTree<Integer> loadTest;

	private TournamentTree<Integer> buildTestee(int nodeCount) {
		TournamentTree<Integer> rv = new TournamentTree<Integer>(1);
		for (int i = 2; i <= nodeCount; i++)
			rv = rv.add(i);
		return rv;
	}

	@Before
	public void setUp() throws Exception {
		singleNode = buildTestee(1);
		twoNodes = buildTestee(2);
		twoLowestLevelGames = buildTestee(10);
		fifteenGreencards = buildTestee(17);
		loadTest = buildTestee(42000);
	}

	@Test
	public void testGetLowestLevelId() {
		assertEquals(0, singleNode.getLowestLevelId());
		assertEquals(1, twoNodes.getLowestLevelId());
		assertEquals(8, twoLowestLevelGames.getLowestLevelId());
		assertEquals(16, fifteenGreencards.getLowestLevelId());
		assertEquals(32768, loadTest.getLowestLevelId());
	}

	@Test
	public void testGetGreencardLevelId() {
		assertEquals(0, singleNode.getGreencardLevelId());
		assertEquals(0, twoNodes.getGreencardLevelId());
		assertEquals(4, twoLowestLevelGames.getGreencardLevelId());
		assertEquals(8, fifteenGreencards.getGreencardLevelId());
		assertEquals(16384, loadTest.getGreencardLevelId());
	}

	private static ArrayList<Integer> greencardLvl(
		TournamentTree<Integer> t) {
		return t.getTournamentPlan(t.getGreencardLevelId());
	}

	private static ArrayList<Integer> lowestLvl(TournamentTree<Integer> t) {
		return t.getTournamentPlan(t.getLowestLevelId());
	}

	@Test(expected=IllegalStateException.class)
	public void getGreencardTournamentPlanForSingleNode() {
		greencardLvl(singleNode);
	}

	@Test(expected=IllegalStateException.class)
	public void getLowestLevelTournamentPlanForSingleNode() {
		lowestLvl(singleNode);
	}

	// Gemeinsamer Test fuer add() und getTournamentPlan();
	// add() wird indirekt getestet, indem wir pruefen, ob der Baum so aussieht
	// wie er soll
	@Test
	public void testGetTournamentPlan() {
		assertEquals(Arrays.asList((Integer)null),
				greencardLvl(twoNodes));
		assertEquals(Arrays.asList(1, 2),
				lowestLvl(twoNodes));

		assertEquals(Arrays.asList(null, null),
			fifteenGreencards.getTournamentPlan(1));
		assertEquals(Arrays.asList(null, null, null, null),
			fifteenGreencards.getTournamentPlan(2));
		assertEquals(Arrays.asList(null, null, null, null,
		                           null, null, null, null),
			fifteenGreencards.getTournamentPlan(4));
		assertEquals(Arrays.asList(null, 9, 5, 13, 3, 11, 7, 15,
				2, 10, 6, 14, 4, 12, 8, 16),
				greencardLvl(fifteenGreencards));
		assertEquals(Arrays.asList(1, 17),
				lowestLvl(fifteenGreencards));
	}
}
