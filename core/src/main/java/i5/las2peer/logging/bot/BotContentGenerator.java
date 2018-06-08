package i5.las2peer.logging.bot;

public interface BotContentGenerator {

	public boolean trainStep(String input, String output);

	public boolean train(String out_dir, double learning_rate, int num_training_steps);

	public Object inference(String input);

}
