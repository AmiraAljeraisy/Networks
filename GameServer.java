import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class GameServer {
    private static final int PORT = 7777;
    private static final int MAX_PLAYERS = 3; // max num for Player
    private static final int WAITING_TIME_SECONDS = 30;

    
    private static Map<String, PrintWriter> players = new HashMap<>();
    private static List<String> waitingRoom = new ArrayList<>(); // waiting Room for Player
    private static List<String> playingRoom = new ArrayList<>();  
    private static Map<String, Integer> playerScores = new HashMap<>(); // To track scores
    private static Map<String, List<String>> wordSynonyms; 
    private static String currentWord;
    private static List<String> currentSynonyms;
    private static List<String> usedWords = new ArrayList<>();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static ScheduledFuture<?> startGameTask;
    private static boolean winner = true;

    public static void main(String[] args) {
    	
    	initializeWords();
    	
        System.out.println("Waiting for player connections...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New player connected.");
                new GameServerThread(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void initializeWords() {
        wordSynonyms = new HashMap<>();
       wordSynonyms.put("سعيد", Arrays.asList("منبسط","مرح", "راضي", "مبتهج", "فرح", "مسرور"));
        wordSynonyms.put("حزين", Arrays.asList("جامعي","مهموم","متضايق","مهموم", "زعلان", "مكتئب", "تعيس", "بائس"));
        wordSynonyms.put("سريع", Arrays.asList("عجول","نشيط", "مسرع", "طيارة", "عاجل", "مستعجل"));
        wordSynonyms.put("بطيء", Arrays.asList("خامل", "سلحفاة", "متأني", "كسول", "متمهل"));
        wordSynonyms.put("جميل", Arrays.asList("نورة بن سعيد","نوره بن سعيد","انتي","فاتن","لافت","وسيم", "بهي", "رائع", "فاتن", "جذاب"));
        wordSynonyms.put("ذكي", Arrays.asList("فطن", "نبيه", "حصيف", "عبقري", "كونان"));
        wordSynonyms.put("قوي", Arrays.asList("صارم","شديد", "متين", "صلب", "صلد", "حاد"));
        wordSynonyms.put("كبير", Arrays.asList("رحيب","ضخم", "عظيم", "هائل", "واسع", "فسيح"));
        wordSynonyms.put("صغير", Arrays.asList("دقيق", "طفيف", "متناه", "قليل", "نتفه"));
        wordSynonyms.put("قريب", Arrays.asList("ملاصق", "محاذي", "مجاور", "محدود", "متاخم"));
        wordSynonyms.put("بعيد", Arrays.asList("منعزل", "نائي", "منفصل", "مقطوع", "قلهات"));
        wordSynonyms.put("خفيف", Arrays.asList("ريشة","ريشة","رقيق", "لطيف", "ضعيف", "رشيق", "حثيث"));
        wordSynonyms.put("ثقيل", Arrays.asList("كثيف", "متين", "ضخم", "حمل", "كبير"));
        wordSynonyms.put("بارد ", Arrays.asList("قارس","قارص","متجمد", "زمهرير", "جامد", "صقيع", "ثلج"));
        wordSynonyms.put("حار", Arrays.asList("دافئ", "حارق", "حميم", "حامي", "ساخن"));
        wordSynonyms.put("صبور", Arrays.asList("طالب","حليم", "متأني", "واسع الصدر", "متمالك", "متحمل"));
        wordSynonyms.put("نفد", Arrays.asList("انقضى","فرغ", "منتهي", "انقطع", "خلص", "انتهى"));
        wordSynonyms.put("الودق", Arrays.asList("المطر","مطر", "الغيث", "غيث", "الندى", "ندى"));
        wordSynonyms.put("فظ", Arrays.asList("متجهم","وقح", "جلف", "عدواني", "سليط", "متغطرس"));
        wordSynonyms.put("الحض", Arrays.asList("الحث","حث", "التحفيز", "تحفيز", "تشجيع", "استثار"));
        wordSynonyms.put("الغيظ", Arrays.asList("الغضب","الحنق", "غضب", "الغل", "حقد", "سخط"));
        wordSynonyms.put("حظر", Arrays.asList("منع","ممنوع", "توقيف", "تحريم", "نهى", "نهي"));        // Add more synonym 
    }

    private static void selectRandomWord() {
        Random random = new Random();
        List<String> words = new ArrayList<>(wordSynonyms.keySet());

        // Remove already used words
        words.removeAll(usedWords);
        
        currentWord = words.get(random.nextInt(words.size()));
        currentSynonyms = new ArrayList<>(wordSynonyms.get(currentWord)); // Create a copy
    
        // Add the selected word to the used list
        usedWords.add(currentWord);
    }
     
    

    static class GameServerThread extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String playerName;
        private static boolean endGame = false;
        
      
        public GameServerThread(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {

                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                playerName = in.readLine();  
                System.out.println("Player " + playerName + " connected.");

                synchronized (players) {
                	players.put(playerName, out);  
                }

                broadcastMessage("Connected players: " + getPlayerNames());
               
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("لعب")) {
                    	handlePlayRequest();
                    	
                    } else if (message.startsWith("synonym:")) {// Handle player Submission
                        String synonym = message.split(":")[1].trim();
                        handleSynonymSubmission(playerName, synonym);
                      
                    }else if(message.startsWith("end")){  // Handle end timer
                    	endGame = true;
                    	startRound();
                    	
                }else if (message.equals("مغادرة")) { // Handle player leaving
                    System.out.println("Player " + playerName + " has requested to leave.");
                    handlePlayerLeave();
                    break; // Exit loop if player wants to leave
             }   
                
                }  
                
            } catch (IOException e) {
                System.out.println("Connection reset: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }
        
        private void broadcastMessage(String message) {
            synchronized (players) {
                for (PrintWriter writer : players.values()) {
                    writer.println(message);
                }
            }
        }

       
        private String getPlayerNames() {
            return String.join(", ", players.keySet());
        }

        private String getWaitingList() {
            return String.join(", ", waitingRoom);
        }
        
        private void handlePlayRequest() {
            synchronized (waitingRoom) {
                String msg = addPlayerToWaitingRoom(playerName);
                out.println(msg);

                if (msg.equals("You have joined the waiting Room.")) {
                    System.out.println("Player " + playerName + " wants to play.");
                    broadcastMessage("Player " + playerName + " joined the waiting list.");
                    broadcastMessage("Waiting list: " + getWaitingList());
                    checkStartConditions(); // Check if game should start

                }
            }
        }
        
        private static synchronized String addPlayerToWaitingRoom(String playerName) {
            if (waitingRoom.size() < MAX_PLAYERS) {
                waitingRoom.add(playerName); // Add the player to the waiting room
                return "You have joined the waiting Room.";
            } else {
                return "Room is full. You cannot join the game."; // If the room is full
            }
        }
        
        
        private void checkStartConditions() {
        	
            if (waitingRoom.size() == MAX_PLAYERS) {
                startGame(); 
            } else if (waitingRoom.size() > 1 && (startGameTask == null || startGameTask.isCancelled())) {
                startGameTask = scheduler.schedule(this::startGame, WAITING_TIME_SECONDS, TimeUnit.SECONDS);
                System.out.println("Timer started for game start in " + WAITING_TIME_SECONDS + " seconds.");
                broadcastMessage("The game will start in " + WAITING_TIME_SECONDS + " seconds if no more players join.");
            }
        }
        
        
        private void startGame() {
            System.out.println("Starting game with players: " + getWaitingList());
            synchronized (waitingRoom) {
                if (startGameTask != null) {
                    startGameTask.cancel(false);
                    startGameTask = null;
                }
               playingRoom.addAll(waitingRoom);
               waitingRoom.clear();
               
               
               broadcastMessage("Game starting...");
               broadcastMessage("Scores: " + getInitialScores());
               
               startRound();
               
            }
        }
        
        private String getInitialScores() {
            synchronized (playerScores) {
                StringBuilder scores = new StringBuilder();
                for (String player : playingRoom) {
                    playerScores.put(player, 0); // Initialize scores to zero
                    scores.append(player).append(": 0, ");
                }

                if (scores.length() > 0) {
                    scores.setLength(scores.length() - 2); // Remove trailing comma and space
                }
                return scores.toString();
            }
        }
        
       
        private String getUpdatedScores() {
            synchronized (playerScores) {
                StringBuilder scores = new StringBuilder();
                for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
                    scores.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
                }

                if (scores.length() > 0) {
                    scores.setLength(scores.length() - 2); // Remove trailing comma and space
                }
                return scores.toString();
            }
        }
        
        
        private void startRound() {
        	synchronized (playingRoom) {
        	
            if (!endGame && playingRoom.size() > 1) {
            	 
            	selectRandomWord();
                broadcastMessage("Word: " + currentWord);
            } else {
                announceWinner();
                return;
            }
        	}
        } 
       
        private void handleSynonymSubmission(String playerName, String synonym) {
        	synchronized (playerScores) {
                if (currentSynonyms.contains(synonym)) {
                    playerScores.put(playerName, playerScores.getOrDefault(playerName, 0) + 1); // Increment score
                    currentSynonyms.remove(synonym); // Remove the used synonym
                    sendMessageToPlayer(playerName,"Valid synonym! scores awarded.");
                    broadcastMessage("Updated Scores: " + getUpdatedScores());
                    startRound();
                } else {
                	sendMessageToPlayer(playerName,"Invalid synonym \n" );
                }
        	}
        }
        
        private void sendMessageToPlayer(String playerName, String message) {
            PrintWriter writer = players.get(playerName);
            if (writer != null) {
                writer.println(message);
            }
        }
       

        private void announceWinner() {

        	if (playingRoom.size() == 1) {
                String winner = playingRoom.get(0);
                broadcastMessage("Game over! Winner: " + winner);
                resetGame();
                return;
            }

        	synchronized (playerScores) {
                int maxScore = playerScores.values().stream().max(Integer::compare).orElse(0);
                List<String> winners = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
                    if (entry.getValue() == maxScore && maxScore > 0) {
                        winners.add(entry.getKey());
                    }
                }

                if (winners.isEmpty() && winner ) {
                    // No winner scenario
                	winner = false;
                	broadcastMessage("Game over! No winner.");
                	
                } else if (winners.size() == 1 && winner) {
                    // Single winner scenario
                	winner = false;
                    String winner = winners.get(0);
                    broadcastMessage("Game over! Winner: " + winner);
                   

                } else if (winners.size() > 1 && winner) {
                    // Tie scenario
                	winner = false;
                	broadcastMessage("Game over! It's a tie! Winners: " + String.join(" & ", winners));
                	

                }

                // Reset game state for the next round
            	resetGame();
 
            }
        
        }

    

        private void resetGame() {
            playingRoom.clear();
            playerScores.clear();
        	usedWords.clear(); // Clear used words for a new round

        }
        
        private void handlePlayerLeave() {
            synchronized (players) {
                players.remove(playerName);
                waitingRoom.remove(playerName);
                playingRoom.remove(playerName);
                playerScores.remove(playerName);
                broadcastMessage("Connected players: " + getPlayerNames());
               
               
                broadcastMessage(playerName + " has left the game.");
                
                
                if (playingRoom.size() > 0) {
                    broadcastMessage("Scores: " + getUpdatedScores());
                }
                
                if (playingRoom.size() == 1) {
                    String lastPlayer = playingRoom.get(0);
                    broadcastMessage("Game ending as only one player remains: " + lastPlayer);
                    broadcastMessage("Congratulations " + lastPlayer + "! You are the last player in the game.");
                    playingRoom.clear(); // Clear the playing room after notifying players
                    playerScores.clear();
                }
            }
        }
        
        
        private void closeConnection() {
            try {
            	handlePlayerLeave();
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
                System.out.println(playerName +" disconnected.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}






