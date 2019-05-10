import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Pranav Patel and Duncan Wilson
 * @ID 150380910 and 150322290
 * @Purpose class for Assignment 1
 *
 */
public class Server {

	public static int connectionSocket;
	public static int width;
	public static int height;
	public static String output_message;
	public static int number_pins;
	public static int number_posts;

	// 1d array holding the colors
	public static List<String> colors = new ArrayList<>();
	/**
	 * 2d dynamic array to hold properties of the note column | 0 = lower x
	 * coordinate | 1 = lower y coordinate | 2 = upper x coordinate | 3 = upper y
	 * coordinate | 4 = color | 5 = message | 6 = pin status rows = different posts
	 */
	public static List<String[]> all_posts = new ArrayList<>();
	/**
	 * 2d dynamic array to hold all the pin points column | 0 = x coordinate | 1 = y
	 * coordinate rows = different pins
	 */
	public static List<int[]> all_pins = new ArrayList<>();
	/**
	 * Logs all the inputs from all clients column | 0 = client ID | rest are part
	 * of the message rows = different inputs
	 */
	public static List<String[]> client_log = new ArrayList<>();

	public static void main(String[] args) throws Exception {
		int clientNumber = 0;
		connectionSocket = Integer.parseInt(args[0]);
		width = Integer.parseInt(args[1]);
		height = Integer.parseInt(args[2]);
		for (int i = 0; i < args.length; i++) {
			if (i >= 3) {
				colors.add(args[i]);
			}
		}

		ServerSocket listener = new ServerSocket(connectionSocket);
		System.out.println("The post board server now running.");
		try {
			while (true) {
				new PostBoard(listener.accept(), clientNumber++).start();
			}
		} finally {
			listener.close();
		}
	}

	private static class PostBoard extends Thread {
		private Socket socket;
		private int clientNumber;

		public PostBoard(Socket socket, int clientNumber) {
			this.socket = socket;
			this.clientNumber = clientNumber;
			log("Client " + clientNumber + " connected at " + socket + ".");

		}

		/**
		 * Services this thread's client by first sending the client a welcome message
		 * then repeatedly reading strings and completing the request
		 */
		public void run() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

				// connection message
				String colors_list = String.join(" ", colors);
				output_message = "Hello client #" + clientNumber + ". The board is " + width + " by " + height
						+ ". The available colors are: " + colors_list;
				toClient(out);
				break_input(in, out);

			} catch (IOException e) {
				log("Error handling client# " + clientNumber + ": " + e);
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					log("Something happened, could not close socket RIP");
				}
				log("Connection with client# " + clientNumber + " closed");
			}
		}

		/**
		 * 
		 * @param in
		 * @param out
		 * @throws IOException
		 * 
		 *                     read input from client line by line and call the methods
		 *                     to process request
		 */
		public void break_input(BufferedReader in, PrintWriter out) throws IOException {
			String first_word[];
			// Get messages from the client, line by line
			while (true) {
				String input = in.readLine().trim();
				if (input == null || input.isEmpty() || input.equals("disconnect")) {
					break;
				}
				// logging the input and finding the type of request
				client_log.add(new String[] { Integer.toString(clientNumber), input });

				// because the first word contains the type of request it can be used to go into
				// the correct methods
				first_word = input.split(" ", 2);
				first_word[0] = first_word[0].toLowerCase().trim();
				if (first_word[0].equals("post")) {
					post(first_word[1]);
				} else if (first_word[0].equals("get")) {
					if (first_word.length < 2) {
						output_message = "Error! No GET operation stated";
					} else {
						get(first_word[1]);
					}
				} else if (first_word[0].contains("pin")) { // both unpin and pin go here
					process_pin(first_word[0], first_word[1]);
				} else if (first_word[0].equals("clear")) {
					clear();
				} else {
					output_message = "Please check input";
				}

				toClient(out); // output
			}
		}

		/**
		 * 
		 * @param input post method to complete the post request from client
		 */
		public void post(String input) {
			int negative_check = 0;
			String color = null, message;
			int lower_x, lower_y, upper_x, upper_y, index = 0;
			String words[] = input.split(" ");
			List<String> message_words = new ArrayList<>();
			lower_x = Integer.parseInt(words[0]);
			lower_y = Integer.parseInt(words[1]);
			upper_x = lower_x + Integer.parseInt(words[2]); // add the width to x coordinate
			upper_y = lower_y + Integer.parseInt(words[3]); // add the height to y coordinate
			if (upper_x - lower_x <= 0 || upper_y - lower_y <= 0) {
				negative_check = 1;
			}
			if (words[4].equals("null")) {
				color = colors.get(0);
			} else {
				color = words[4];
			}
			int found_color = 0;
			for (String str : colors) {
				if (str.trim().contains(color)) {
					found_color = 1;
				}
			}
			for (int i = 5; i < words.length; i++) {
				// System.out.println(words.length);
				message_words.add(words[i]);
				index++;
			}
			message = String.join(" ", message_words);

			// check if post is valid on board and continue or go to post error
			if (found_color != 1) { // color not in list
				output_message = "Please input a color from the list of colors";
			} else if (lower_x > 0 && lower_y > 0 && upper_x <= width && upper_y <= height && negative_check == 0) {
				all_posts.add(new String[] { Integer.toString(lower_x), Integer.toString(lower_y),
						Integer.toString(upper_x), Integer.toString(upper_y), color, message, "0" });
				output_message = "Note successfully posted to the board";
			} else if (negative_check == 1) {
				output_message = "Please do not enter negative values";
			} else { // post out of board
				output_message = "Can't post note on this board (placement out of the board)";
			}
		}

		/**
		 * 
		 * @param input Monster get method to complete all the different type of get
		 *              requests from client
		 */
		public void get(String input) {
			int negative_check = 0;
			int found = 0;
			String first_word[] = input.split(" ", 2);
			String post_info = "", color_find = "", referto = "", pin_info = "", p_message, p_color;
			number_pins = all_pins.size();
			number_posts = all_posts.size();
			int x = 0, y = 0, points = 1, refers = 1, has_color = 1;
			int lx, ly, ux, uy;
			String input_break[] = input.split(" ");
			// show all pins
			if (first_word[0].trim().contains("pin")) {
				if (number_pins > 0) {
					for (int i = 0; i < number_pins; i++) {
						pin_info = pin_info + " (" + all_pins.get(i)[0] + " , " + all_pins.get(i)[1] + ")";
					}
					output_message = "Here are the pin locations: " + String.join(" ", pin_info);
				} else {
					output_message = "No pins to display";
				}
			}
			// else will have color,content, and refersTo fields
			else {
				if (!input_break[0].equals("null")) {
					color_find = input_break[0];
				} else {
					has_color = 0;
				}
				if (!input_break[1].equals("null")) {
					x = Integer.parseInt(input_break[1]);
					y = Integer.parseInt(input_break[2]);
					if (x < 0 || y < 0) {
						negative_check = 1;
					}
					if (!input_break[3].equals("null")) {
						if (input_break[3].contentEquals("-null")) {
							input_break[3] = input_break[3].substring(1);
						}
						for (int i = 0; i < input_break.length; i++) {
							if (i >= 3) {
								referto = "" + referto + input_break[i];
							}
						}
					} else {
						refers = 0;
					}
				} else {
					points = 0;
					if (!input_break[2].equals("null")) {
						if (input_break[2].equals("-null")) {
							input_break[2] = input_break[2].substring(1);
						}
						for (int i = 0; i < input_break.length; i++) {
							if (i >= 2) {
								referto = "" + referto + input_break[i];
							}
						}
					} else {
						refers = 0;
					}
				}
				if (number_posts > 0) {
					for (int i = 0; i < number_posts; i++) { // this is not efficient way of looping but it lessen the
																// number of
																// lines written LOL
						lx = Integer.parseInt(all_posts.get(i)[0]);
						ly = Integer.parseInt(all_posts.get(i)[1]);
						ux = Integer.parseInt(all_posts.get(i)[2]);
						uy = Integer.parseInt(all_posts.get(i)[3]);
						p_color = all_posts.get(i)[4];
						p_message = all_posts.get(i)[5];
						// get the posts with the correct properties
						if (has_color == 1 && points == 0 && refers == 0) { // just color
							if (p_color.equals(color_find)) {
								post_info = get_Post_Info(post_info, i);
								found++;
							}
						} else if (has_color == 1 && points == 1 && refers == 0) {
							if (p_color.equals(color_find) && x >= lx && y >= ly && x <= ux && y <= uy) { // just color
																											// and
																											// coordinate
								post_info = get_Post_Info(post_info, i);
								found++;
							}
						} else if (has_color == 1 && points == 1 && refers == 1) { // all 3
							if (p_color.equals(color_find) && x >= lx && y >= ly && x <= ux && y <= uy
									&& p_message.contains(referto)) { // just color and coordinate
								post_info = get_Post_Info(post_info, i);
								found++;
							}
						} else if (has_color == 0 && points == 1 && refers == 0) { // just coordinates
							if (x >= lx && y >= ly && x <= ux && y <= uy) { // just color and coordinate
								post_info = get_Post_Info(post_info, i);
								found++;
							}
						} else if (has_color == 0 && points == 1 && refers == 1) { // just coordinates and substring
							if (x >= lx && y >= ly && x <= ux && y <= uy && p_message.contains(referto)) { // just color
																											// and
																											// coordinate
								post_info = get_Post_Info(post_info, i);
								found++;
							}
						} else if (has_color == 0 && points == 0 && refers == 1) { // just substring
							if (p_message.contains(referto)) { // just color and coordinate
								post_info = get_Post_Info(post_info, i);
								found++;
							}
						} else if (has_color == 1 && points == 0 && refers == 1) { // just color and substring
							if (p_color.equals(color_find) && p_message.contains(referto)) { // just color and
																								// coordinate
								post_info = get_Post_Info(post_info, i);
								found++;
							}
						} else { // no input -> ERROR
							post_info = "Error! No GET operation stated";
						}
					}
				} else {
					post_info = "No notes to display";
				}
				if (found == 0) {
					post_info = "No notes to display";
				}
				if (negative_check == 1) {
					output_message = "Please do not enter negative values";
				} else {
					output_message = post_info;
				}
			}
		}

		/**
		 * 
		 * @param post_info updates the current post_info string to add other posts that
		 *                  need to be "get"
		 */
		public String get_Post_Info(String post_info, int i) {
			String tempx = Integer
					.toString(Integer.parseInt(all_posts.get(i)[2]) - Integer.parseInt(all_posts.get(i)[0])); // reversing
																												// the
																												// addition
																												// to
																												// the
																												// width
			String tempy = Integer
					.toString(Integer.parseInt(all_posts.get(i)[3]) - Integer.parseInt(all_posts.get(i)[1])); // reversing
																												// the
																												// addition
																												// to
																												// the
																												// height
			post_info = post_info + "\n" + all_posts.get(i)[0] + " " + all_posts.get(i)[1] + " " + tempx + " " + tempy
					+ " " + all_posts.get(i)[4] + " " + all_posts.get(i)[5];
			return post_info;
		}

		/**
		 * 
		 * @param type
		 * @param input method to complete the action for pin or unpin
		 */
		public void process_pin(String type, String input) {
			String pin_points[] = input.split(" ");
			int found = 0;
			int pin_x = Integer.parseInt(pin_points[0]);
			int pin_y = Integer.parseInt(pin_points[1]);
			if (pin_x >= 0 && pin_y >= 0) { // means no coordinates are negative
				number_pins = all_pins.size();
				// pin position
				if (number_pins > 0) { // there are pins on board if here
					for (int i = 0; i < number_pins; i++) {
						if (all_pins.get(i)[0] == pin_x && all_pins.get(i)[1] == pin_y) { // found pin check what to do
							found = 1;
							if (type.equals("unpin")) {
								all_pins.remove(i); // take pin out and update posts pin status after
								posts_pin_status(type, pin_x, pin_y);
								output_message = "Successful unpin";
								break;
							} else {
								posts_pin_status(type, pin_x, pin_y);
								output_message = "Successful pinned position on another pin";
								break;
							}
						}
					}
				}
				if (found == 0) { // pin not found
					if (type.equals("unpin")) {
						output_message = "Nothing to unpin";
					} else {
						all_pins.add(new int[] { pin_x, pin_y });
						// update status of posts
						posts_pin_status(type, pin_x, pin_y);
						output_message = "Successful pinned position";
					}
				}
			} else {
				output_message = "Please do not enter negative values";
			}
		}

		/**
		 * clears all unpinned posts
		 */
		public void clear() {
			number_posts = all_posts.size();
			if (number_posts > 0) {
				for (int p = 0; p < number_posts; p++) {
					if (all_posts.get(p)[6].trim().equals("0")) { // remove post
						all_posts.remove(p);
					}
				}
			}
			output_message = "Clear successful";
		}

		/**
		 * updates pin status after a pin or unpin has been made
		 */
		public void posts_pin_status(String type, int pin_x, int pin_y) {
			number_posts = all_posts.size();
			number_pins = all_pins.size();
			int lx, ly, ux, uy, pins, pins_on_post = 0;
			String updated[], status_p = "";
			// first change status to not pinned
			if (number_posts > 0) {
				// now go through each post and change status of pin
				for (int p = 0; p < number_posts; p++) {
					lx = Integer.parseInt(all_posts.get(p)[0]);
					ly = Integer.parseInt(all_posts.get(p)[1]);
					ux = Integer.parseInt(all_posts.get(p)[2]);
					uy = Integer.parseInt(all_posts.get(p)[3]);
					pins = Integer.parseInt(all_posts.get(p)[6]);
					if (pin_x >= lx && pin_y >= ly && pin_x <= ux && pin_y <= uy) { // pin hits post
						if (type.equals("unpin") && pins < 2) {
							status_p = "0";
						} else if (type.equals("unpin") && pins > 1) { // multiple pins on post just remove 1
							status_p = Integer.toString(pins - 1);
						} else {
							// go through each pin and find out how many hit the post that are not current
							// pin
							// if 0 pins hit the post and pins number is > 0 it means that this post was
							// pinned by current pin don't do anything
							// if >0 pins hit the post and pins number == that number it means this pin has
							// not previously hit that post so add 1 to pins
							// if >0 pins hit the post and pins number > that number it means that this pin
							// has previously hit that post so don't do anything
							for (int i = 0; i < number_pins; i++) {
								int temp_x = all_pins.get(i)[0];
								int temp_y = all_pins.get(i)[1];
								if (temp_x != pin_x && temp_y != pin_y && temp_x >= lx && temp_y >= ly && temp_x <= ux
										&& temp_y <= uy) { // means this pin is on post already so add 1 to pin_on_post
									pins_on_post++;
								}
							}
							if (pins_on_post == pins) { // means current pin has not hit this post so must pin it and
														// add 1
								status_p = Integer.toString(pins + 1);
							}
						}
						updated = all_posts.get(p);
						updated[6] = status_p; // updated pin status
						all_posts.set(p, updated); // update all_posts array
					}
				}
			}
		}

		/**
		 * 
		 * @param out output message to client about status of request
		 */
		public void toClient(PrintWriter out) {
			// output to client about request status
			out.println(output_message);
			out.println(" ");
			// reset message
			output_message = "Please check input";
		}

		/**
		 * Logs a simple message. In this case we just write the message to the server
		 * applications standard output.
		 */
		private void log(String message) {
			System.out.println(message);
		}
	}
}