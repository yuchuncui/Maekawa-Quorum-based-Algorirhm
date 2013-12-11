public class Grouping {

	public static void main(String args[]) {
		int n = 21;
		int k = (int) ((1 + Math.sqrt(4 * n + 1)) / 2);
		int[][] intersections = new int[n][k];
		int counter = 0, timer = 0;
		while (timer < k) {
			for (int j = 0; j < n; j++) {
				intersections[timer][0] = 0;
			}
			timer++;
		}
		for (int l = 0, temp_counter = 1; l < k; l++) {
			for (int j = 1; j < k; j++) {
				intersections[l][j] = temp_counter;
				temp_counter++;
				if (j == k) {
					j = 1;
					l++;
				}
			}
		}

		int pieces = (n - k) / (k - 1);// remain, how many per group
		while (timer < n) {
			for (int j = 0; j < pieces; j++) {
				for (int m = 0; m < pieces; m++) {
					intersections[timer][0] = counter + j + 1;
					timer++;
				}
			}
		}

		for (int i = pieces + 1, inc = 1, col = 1; i < n; i++) {
			int remain = col % pieces;
			if (remain == 0) {
				remain = pieces;
			}
			int start_row = pieces + inc;
			for (int e = 0, row = start_row, temp = 0; temp < pieces; e += remain, temp++, row += pieces) {
				int temp_row = row + e;
				while (temp_row > ((temp + 2) * pieces)) {
					temp_row -= pieces;
				}
				intersections[temp_row][col] = i;
			}
			inc++;
			if (i % pieces == 0) {
				col++;
				inc = 1;
			}
		}

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < k; j++) {
				System.out.print(intersections[i][j] + " ");
			}
			System.out.print("\n");
		}
	}
}
