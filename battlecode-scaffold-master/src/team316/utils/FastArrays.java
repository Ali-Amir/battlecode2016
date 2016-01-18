package team316.utils;

import team316.RobotPlayer;

public class FastArrays {
	// Implementing Fisher–Yates shuffle. Taken from:
	// http://stackoverflow.com/questions/1519736/random-shuffling-of-an-array
	public static void shuffle(PairIntDouble[] ar) {
		for (int i = ar.length - 1; i > 0; i--) {
			int index = RobotPlayer.rnd.nextInt(i + 1);
			// Simple swap
			PairIntDouble a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}

	public static void quickSort(PairIntDouble[] a, int l, int r) {
		if (r - l <= 1) {
			return;
		}

		double midVal = a[RobotPlayer.rnd.nextInt(r - l) + l].y;
		int i = l, j = r - 1;
		for (; i < j;) {
			for (; i < r && a[i].y <= midVal; ++i);
			for (; j > l - 1 && a[j].y > midVal; --j);
			if (i < j) {
				PairIntDouble x = a[i];
				a[i] = a[j];
				a[j] = x;
				++i;
				--j;
			}
		}

		if (i < r) {
			quickSort(a, l, i);
		}
		if (i > l) {
			quickSort(a, i, r);
		}
	}

	public static void quickSort(PairIntDouble[] a) {
		quickSort(a, 0, a.length);
	}
}