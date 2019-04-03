package org.forome.annotation.matcher;

import com.google.common.collect.Lists;

import java.util.*;

public class SequenceMatcher {

	private final String a;
	private final String b;
	private HashMap<Character, List<Integer>> b2j;

	public SequenceMatcher(String a, String b) {
		this.a = a;
		this.b = b;
		__chain_b();
	}

	private void __chain_b() {
		String b = this.b;
		int n = b.length();
		this.b2j = new HashMap<>();
		HashMap<Character, Integer> populardict = new HashMap<>();
		for (int i = 0; i < b.length(); i++) {
			char elt = b.charAt(i);
			if (b2j.containsKey(elt)) {
				List<Integer> indices = new ArrayList<>();
				indices = b2j.get(elt);
				if (n >= 200 && indices.size() * 100 > n) {
					populardict.put(elt, 1);
				} else {
					indices.add(i);
				}
			} else {
				b2j.put(elt, Lists.newArrayList(i));
			}
		}

		for (Character elt : populardict.keySet()) {
			b2j.remove(elt);
		}
	}

	public List<Tuple3<Integer, Integer, Integer>> getMatchingBlocks() {
		int la = a.length();
		int lb = b.length();

		List<Tuple4<Integer, Integer, Integer, Integer>> queue = new ArrayList();
		queue.add(Tuple4.valueOf(0, la, 0, lb));

		List<Tuple3<Integer, Integer, Integer>> matching_blocks = new ArrayList();
		while (!queue.isEmpty()) {
			Tuple4<Integer, Integer, Integer, Integer> val = queue.remove(0);
			int alo = val.value0;
			int ahi = val.value1;
			int blo = val.value2;
			int bhi = val.value3;
			Tuple3<Integer, Integer, Integer> x = findLongestMatch(alo, ahi, blo, bhi);
			int i = x.value0;
			int j = x.value1;
			int k = x.value2;
			/*
            # a[alo:i] vs b[blo:j] unknown
            # a[i:i+k] same as b[j:j+k]
            # a[i+k:ahi] vs b[j+k:bhi] unknown
            */
			if (k > 0) {   // if k is 0, there was no matching block
				matching_blocks.add(x);
				if (alo < i && blo < j) {
					queue.add(Tuple4.valueOf(alo, i, blo, j));
				}
				if (i + k < ahi && j + k < bhi) {
					queue.add(Tuple4.valueOf(i + k, ahi, j + k, bhi));
				}

			}
		}

		matching_blocks.sort(Comparator.comparing(o -> o.value0));

        /*
        # It's possible that we have adjacent equal blocks in the
        # matching_blocks list now.  Starting with 2.5, this code was added
        # to collapse them.
        */
		int i1 = 0;
		int j1 = 0;
		int k1 = 0;
		List<Tuple3<Integer, Integer, Integer>> non_adjacent = new ArrayList<>();
		for (Tuple3<Integer, Integer, Integer> item : matching_blocks) {
			int i2 = item.value0;
			int j2 = item.value1;
			int k2 = item.value2;
			//# Is this block adjacent to i1, j1, k1?
			if (i1 + k1 == i2 && j1 + k1 == j2) {
            	/*
                # Yes, so collapse them -- this just increases the length of
                # the first block by the length of the second, and the first
                # block so lengthened remains the block to compare against.
                */
				k1 += k2;
			} else {
            	/*
                # Not adjacent.  Remember the first block (k1==0 means it's
                # the dummy we started with), and make the second block the
                # new block to compare against.
                */
				if (k1 > 0) {
					non_adjacent.add(Tuple3.valueOf(i1, j1, k1));
				}

				i1 = i2;
				j1 = j2;
				k1 = k2;
			}
		}

		if (k1 > 0) {
			non_adjacent.add(Tuple3.valueOf(i1, j1, k1));
		}

		non_adjacent.add(Tuple3.valueOf(la, lb, 0));
		matching_blocks = non_adjacent;
		return matching_blocks;
	}

	public Tuple3<Integer, Integer, Integer> findLongestMatch(int alo, int ahi, int blo, int bhi) {
    	/*
        # CAUTION:  stripping common prefix or suffix would be incorrect.
        # E.g.,
        #    ab
        #    acab
        # Longest matching block is "ab", but if common prefix is
        # stripped, it's "a" (tied with "b").  UNIX(tm) diff does so
        # strip, so ends up claiming that ab is changed to acab by
        # inserting "ca" in the middle.  That's minimal but unintuitive:
        # "it's obvious" that someone inserted "ac" at the front.
        # Windiff ends up at the same place as diff, but by pairing up
        # the unique 'b's and then matching the first two 'a's.
        */

		int besti = alo;
		int bestj = blo;
		int bestsize = 0;
        /*
        # find longest junk-free match
        # during an iteration of the loop, j2len[j] = length of longest
        # junk-free match ending with a[i-1] and b[j]
        */
		Map<Integer, Integer> j2len = new HashMap<Integer, Integer>();
		List<Integer> nothing = new ArrayList<>();
		for (int i = alo; i < ahi; i++) {

        	/*
            # look at all instances of a[i] in b; note that because
            # b2j has no junk keys, the loop is skipped if a[i] is junk
            */
			Map<Integer, Integer> newj2len = new HashMap<Integer, Integer>();
			for (int j : b2j.getOrDefault(a.charAt(i), Collections.emptyList())) {
				// a[i] matches b[j]
				if (j < blo) {
					continue;
				}
				if (j >= bhi) {
					break;
				}

				if (j2len.containsKey(j - 1)) {
					newj2len.put(j, j2len.get(j - 1) + 1);
				} else {
					newj2len.put(j, 1);
				}

				int k = newj2len.get(j);
				if (k > bestsize) {
					besti = i - k + 1;
					bestj = j - k + 1;
					bestsize = k;
				}
			}
			j2len = newj2len;
		}

        /*
        # Extend the best by non-junk elements on each end.  In particular,
        # "popular" non-junk elements aren't in b2j, which greatly speeds
        # the inner loop above, but also means "the best" match so far
        # doesn't contain any junk *or* popular non-junk elements.
        */
		while (besti > alo && bestj > blo && a.charAt(besti - 1) == b.charAt(bestj - 1)) {
			besti = besti - 1;
			bestj = bestj - 1;
			bestsize = bestsize + 1;
		}

		while (besti + bestsize < ahi && bestj + bestsize < bhi && a.charAt(besti + bestsize) == b.charAt(bestj + bestsize)) {
			bestsize += 1;
		}

        /*
        # Now that we have a wholly interesting match (albeit possibly
        # empty!), we may as well suck up the matching junk on each
        # side of it too.  Can't think of a good reason not to, and it
        # saves post-processing the (possibly considerable) expense of
        # figuring out what to do with it.  In the case of an empty
        # interesting match, this is clearly the right thing to do,
        # because no other kind of match is possible in the regions.
        */
		while (besti > alo && bestj > blo && a.charAt(besti - 1) == b.charAt(bestj - 1)) {
			besti = besti - 1;
			bestj = bestj - 1;
			bestsize = bestsize + 1;
		}

		while (besti + bestsize < ahi && bestj + bestsize < bhi && a.charAt(besti + bestsize) == b.charAt(bestj + bestsize)) {
			bestsize = bestsize + 1;
		}

		return Tuple3.valueOf(besti, bestj, bestsize);
	}

	public static final class Tuple2<T0, T1> {

		public final T0 value0;
		public final T1 value1;

		protected Tuple2(T0 value0, T1 value1) {
			this.value0 = value0;
			this.value1 = value1;
		}

		public static <T0, T1> Tuple2<T0, T1> valueOf(T0 value0, T1 value1) {
			return new Tuple2<>(value0, value1);
		}
	}

	public static final class Tuple3<T0, T1, T2> {

		public final T0 value0;
		public final T1 value1;
		public final T2 value2;

		protected Tuple3(T0 value0, T1 value1, T2 value2) {
			this.value0 = value0;
			this.value1 = value1;
			this.value2 = value2;
		}

		public static <T0, T1, T2> Tuple3<T0, T1, T2> valueOf(T0 value0, T1 value1, T2 value2) {
			return new Tuple3<>(value0, value1, value2);
		}
	}

	public static final class Tuple4<T0, T1, T2, T3> {

		public final T0 value0;
		public final T1 value1;
		public final T2 value2;
		public final T3 value3;

		protected Tuple4(T0 value0, T1 value1, T2 value2, T3 value3) {
			this.value0 = value0;
			this.value1 = value1;
			this.value2 = value2;
			this.value3 = value3;
		}

		public static <T0, T1, T2, T3> Tuple4<T0, T1, T2, T3> valueOf(T0 value0, T1 value1, T2 value2, T3 value3) {
			return new Tuple4<>(value0, value1, value2, value3);
		}
	}
}



























