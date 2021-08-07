package com.pixxel.animation;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.Sprite;

/**Manages sprite animations as easy as possible.*/
public class SpriteAnimation {
	
	/**Holds all movies you're adding.*/
    private final ArrayList<Movie> movies = new ArrayList<Movie>(); 
    private Movie currentMovie;
    
    private long frameCount = 0;
    private String currentStatus = "";
    private Sprite currentFrame = null;
    private int currentFrameindex = 0;
    
    private int iteration = 0;
    
    private int major_maxIteration = 0;
    private int major_iteration = 0;
    private boolean majorAnimation = false;
    private boolean locked = false;
    
    private final ArrayList<Movie> chain = new ArrayList<Movie>();
    private int chainIndex = 0;
    private boolean chainAnimation = false;
    
    private String finishedMovie = "";
	private boolean freezed = false;

    public SpriteAnimation(){}
    
    /**Returns the movie with the id you have been added before. <br>
     * Throws an exception if the id does not exist. To prevent this, check with {@link SpriteAnimation#idExist(String)}
     * if this id had been added before.
     * @param id The name of the movie.
     * @throws IllegalAccessError If the movie does not exist.*/
    public final Movie getMovie(String id) throws IllegalAccessError {
    	for(int i = 0; i < movies.size(); i++) {
    		if(id.equals(movies.get(i).getId())) {
    			return movies.get(i);
    		}
    	}
    	throw new IllegalAccessError("This id does not exist!");
    }
    
    /**Checks if the status is added to the library before
     * @param id - How you want to name the sheet.
     * @return True if the status exist in the library.*/
    public final boolean idExist (String id) {   	
    	for(int i = 0; i < movies.size(); i++) {
    		if(id.equals(movies.get(i).getId())){
    		  return true;	
    		}
     	} 
    	return false;
    }
    
    /**Adds a movie into the {@link SpriteAnimation#movies} collection.
     * @param id - The name of the status (Should not be empty!)
     * @param movie - All frames in order the movie should have (Must have at least one frame!)
     * @throws IllegalAccessError If the state name is empty*/
    public final void addMovie (String id, Movie movie) throws IllegalAccessError {
    	if(id.isEmpty() || id == null) throw new IllegalAccessError("The state should have at least a name!");

    	movie.id = id;
    	movies.add(movie);
    }
    
    /**Removes the movie with this id. <br>
     * If it dosen't exist, nothing will happen*/
    public final void removeMovie(String id) {
    	for(int i = 0; i < movies.size(); i++){
    		if(movies.get(i).getId().equals(id)) {
    			resetFramecount();
    			currentFrame = null;
    			movies.remove(i);
    		}
    	}
    }
    
   /**Combines existing movies to one and adds it to the {@link SpriteAnimation#movies} collection.
    * @param id The name of the new, combined movie
    * @param frameRate The framerate
    * @param states All movie id's (They should exist) that should get combinet to one.*/
    public final void combine(String id, int frameRate, String[] states){
	    ArrayList<Sprite> frames = new ArrayList<Sprite>();  //Creates a new movie that adds all movies
    	for(int i = 0; i < states.length; i++){
    		for(int j = 0; j < getMovie(states[i]).frames.size(); j++) {
    			frames.add(getMovie(states[i]).frames.get(j));
    		}
    	}
    	
	    addMovie(id, new Movie(frameRate, frames.toArray(new Sprite[frames.size()])));
   }
    
   public final void createAnimationChain(String[] statusNames) {
	   if(!locked && !chainAnimation) {
	     chain.clear();
	     
	     for(int i = 0; i < statusNames.length; i++) {
	 	    if(idExist(statusNames[i])) {
	 	 	   chain.add(getMovie(statusNames[i]));
	 	    }
	     }
	     
	     play(chain.get(0).getId());
	     locked = true;
	     chainAnimation = true;
	     chainIndex = 0;
	   }
   }
    
    /**Plays this animation in a infinite loop (The old animation is overwritten).<br>
     * You can stop the loop with {@link SpriteAnimation#stop()}<p><b>
     * Be careful! Nothing will happen if the id is typed wrong or doesn't exist! </b>-> causes the old animation to play still.
     * @param id - One id you added before in {@link SpriteAnimation#addMovie(String, Movie)}*/
   public final void play(String id) {
//   		freezed = false;
//   		if(currentStatus.equals(id)) return;
//    	if(idExist(id) && !locked) {
//    	  frameCount = 0;
//    	  iteration = 0;
//    	  currentFrameindex = 0;
//    	  currentStatus = id;
//    	  currentFrame = getMovie(id).frames.get(0); //All movies should have at least one frame
//    	  currentMovie = getMovie(id);
//    	}
	   playAt(id, 0);
    }

    public final void playAt(String id, long frameCount) {
		freezed = false;
		if(currentStatus.equals(id)) return;
		if(idExist(id) && !locked) {
			Movie m = getMovie(id);
			this.frameCount = frameCount;
			iteration = 0;
			currentFrameindex = 0;
			currentStatus = id;
			currentFrame = m.frames.get(currentFrameindex); //All movies should have at least one frame
			currentMovie = m;
		}
	}
   
   /**Plays the movie like {@link SpriteAnimation#play(String)} <br>
    * but it dosen't allow to abort this animation by other loops, until the given max iteration.
    * @param id The movie name
    * @param iterations The maximum amount of iterations this movie should play. <br><b>(0 or less will play only one iteration)</b>*/
   public final void playMajor(String id, int iterations) {
	   play(id);

	   locked = true;
	   majorAnimation = true;
	   major_maxIteration = iterations;
	   major_iteration = 0;
   }
   
   public final void stop() {
	   currentStatus = "";
	   currentFrame = null;
	   frameCount = 0;
	   currentMovie = null;
   }

   /**Locks the animation at its current frame*/
   public final void freeze() {
   		freezed = true;
   }

   public final void unfreeze() {
   		freezed = false;
   }
    
    public final void resetFramecount() {
    	frameCount = 0;
    }

    /**It is only possible to estimate the frameCount outside of the update function,
	 * because the Framerate could change until the actual update function gets called*/
    public int estimateFrameIndex(long frameCount, int currentFPS) {
    	if(currentMovie == null) return -1;
		return (int) (frameCount / (currentFPS / currentMovie.frameRate)) % currentMovie.frames.size();
	}

    /**The 'onUpdate' function. Just call this once every frame!
	 * @param currentFPS - The FPS, the game currently has*/
    public final Sprite update (int currentFPS) {
    	if(!currentStatus.isEmpty() && currentMovie != null) { //Otherwise just no animation   		   				
			finishedMovie = "";

			if (!freezed) {
				frameCount++;
				if (currentFPS  / currentMovie.frameRate != 0) {
//					int divide = currentFPS / currentMovie.frameRate;
//					if (divide != 0) {
						//if (frameCount % divide == 0) currentFrameindex++;
						currentFrameindex = (int) (frameCount / (currentFPS / currentMovie.frameRate));
//					}
				}
				if (currentFrameindex >= currentMovie.frames.size()) {
					if (locked) {
						if (majorAnimation) {
							major_iteration++;

							if (major_iteration >= major_maxIteration) {
								locked = false;
								majorAnimation = false;
								major_maxIteration = 0;
								resetFramecount();
							}
						}

						if (chainAnimation) {
							chainIndex++;

							if (chainIndex < chain.size()) {
								locked = false;
								play(chain.get(chainIndex).getId());
								locked = true;
							} else {
								locked = false;
								chainAnimation = false;
							}
						}
					}


					frameCount = 0;
					iteration++;
					currentFrameindex = 0;
					finishedMovie = currentStatus;
				}

				currentFrame = currentMovie.frames.get(currentFrameindex >= currentMovie.frames.size() ? currentFrameindex = currentMovie.frames.size()-1 : currentFrameindex);
			}
		}
    	return currentFrame;
    }
    
    public final boolean finished(String id) {
    	if(finishedMovie.equals(id)) return true;
    	return false;
    }
    
    /**Shows how many loops one movie had made since the last
     * <br> {@link SpriteAnimation#play(String)} or <br> {@link SpriteAnimation#playMajor(String, int)} call.*/
    public final int getIteration() {
    	return iteration;
    }
    
    /**@return The current frame index from the movie witch is played.
     * <p><b>Returns 0, when no movie is playing</b>*/
    public final int getFrameIndex() {
    	return currentFrameindex;
    }

    public final void setFrameCount(long frameCount) {
    	this.frameCount = frameCount;
	}

	public final long getFrameCount() {
    	return frameCount;
	}

	public final boolean isFreezed() {
    	return freezed;
	}

    public final void setFrameIndex(int frameIndex) {
    	if(frameIndex < 0 || frameIndex > currentMovie.frames.size()) return;
    	this.currentFrameindex = frameIndex;
		currentFrame = currentMovie.frames.get(currentFrameindex);
	}
    
    /**Returns the frame, that is right now in the played movie
     * @return The frame as Sprite <br><b>(returns null if no movie is playing)</b>
     * @see Sprite*/
    public final Sprite getCurrentFrame () {
    	return currentFrame;
    }
    
    /**The name/id of the movie that is currently playing.
     * <p><b>Returns an empty string, if no movie is played.</b>*/
    public final String getCurrentId() {
    	return currentStatus;
    }
    
    /**Clears all movies and resets everything.*/
    public final void clear() {
    	movies.clear();
    	frameCount = 0;
        currentStatus = "";
        currentFrame = null;
    }
}
