package i5.las2peer.logging.bot;

public interface BotContentGenerator {

	public boolean trainStep(String out_dir, String input, String output);

	public boolean train(String out_dir, String unit, double learning_rate, int num_training_steps, int epochs);

	public Object inference(String out_dir, String input);

}
