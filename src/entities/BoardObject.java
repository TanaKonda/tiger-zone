package entities;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ListIterator;
import java.util.Set;

/*
 * This is the BoardObject that will be handling a majority of the game logic.
 * It is representative of the board game itself, and has a statically defined
 * size that is adjustable as needed.
 */
public class BoardObject {

	//BOARD ATTRIBUTES
	public static final int ROWSIZE = 11, COLSIZE = 11;
	public static int startX = 0;
	public static int startY = 0;

	protected TigerTile[][] board;
	protected boolean state; //east now this serves as just a if we started or not
	protected Player[] players;
	protected Player activePlayer;

	protected ArrayList<TigerTile> tiles; //a mapping of all available tiles
	protected Map<Integer, Integer> minSpots; //a map to keep track of the least available spot
	protected ArrayList<Location> availableSpots; //a list of empty spots that surround tiles

	protected Map<Integer, Region> incompleteRegions; //a mapping to keep track of all of the incomplete regions
	protected Map<Integer, Region> allRegions;
	protected Set<Region> completedRegions; //a set of all completed regions
	protected ArrayList<DenRegion> denRegions;

	protected String whyInvalid; //for error reporting
	protected Location recentPlacement; //the most recently placed TigerTile
	protected TigerTile recentTile;
	protected boolean tigerPlaced; //to indicate if a tiger has been placed or not
	protected boolean crocodilePlaced;
	protected boolean pending; //pending confirmation (probably unnecessary)
	protected ArrayList<TilePair> possibleTileSpots;
	protected int move;

	public ArrayList<TilePair> getPossibleSpots() {
		return possibleTileSpots;
	}

	public TigerTile getRecentTile() {
		return recentTile;
	}
	//CONSTRUCTORS

	/**
	 * BoardObject() constructor, initialize the variables
	 */
	public BoardObject() {

		availableSpots = new ArrayList<Location>();
		incompleteRegions = new HashMap<Integer, Region>();
		allRegions = new HashMap<Integer, Region>();
		minSpots = new HashMap<Integer, Integer>();
		completedRegions = new LinkedHashSet<Region>();
		denRegions = new ArrayList<DenRegion>();
		board = new TigerTile[ROWSIZE][COLSIZE];
		pending = false;
		whyInvalid = "";
		possibleTileSpots = new ArrayList<TilePair>();
		move = 0;
	} //end constructor

	/**
	 * BoardObject() constructor, initialize the variables
	 */
	public BoardObject(BoardObject clone) {

		availableSpots = clone.availableSpots;
		incompleteRegions = clone.incompleteRegions;
		allRegions = clone.allRegions;
		minSpots = clone.minSpots;
		completedRegions = clone.completedRegions;
		board = clone.board;
		pending = clone.pending;
		whyInvalid = clone.whyInvalid;
		possibleTileSpots = clone.possibleTileSpots;
		move = clone.move;
		players = clone.players;
		activePlayer = clone.activePlayer;
		tiles = clone.tiles;
		recentPlacement = clone.recentPlacement;
		recentTile = clone.recentTile;
		tigerPlaced = clone.tigerPlaced;
		crocodilePlaced = clone.crocodilePlaced;
		possibleTileSpots = clone.possibleTileSpots;

	} //end constructor

	//ACCESSORS

	public String getReason() {
		String reason = whyInvalid;
		setReason("");
		return reason;
	}

	public Player[] getPlayers() {
		return players;
	}

	public Player getPlayer(int index) {
		return players[index];
	}

	public TigerTile[][] getBoard() {
		return board;
	}

	/**
	 *	getAS() returns the array list of available spots
	 *	@return the ArrayList of available spots
	 */
	public ArrayList<Location> getAS() {
		return availableSpots;
	}

	public Map<Integer, Region> getAll() {
		return allRegions;
	}

	public Map<Integer, Region> getIncomplete() {
		return incompleteRegions;
	}

	public Set<Region> getComplete() {
		return completedRegions;
	}

	public Region getIncompleteRegion(int key) {
		return incompleteRegions.get(key);
	}

	//get the moore neighborhood around the specified location
	public ArrayList<Location> getMoore(Location coord) {

		//adjust ARRAY coordinates based on given starting position
		int row = coord.getY();
		int col = coord.getX();

		ArrayList<Location> mooreHood = new ArrayList<Location>();

		TigerTile center = board[row][col];
		if (center == null) {
			setReason("Can't get Moore neighborhood: given location is empty.");
			return mooreHood;
		}
		mooreHood.add(coord);

		//obtain the neighboring tiles
		TigerTile north = null, east = null, south = null, west = null;
		if(row > 0) north = board[row - 1][col];
		if(col < COLSIZE-1) east = board[row][col + 1];
		if(row < ROWSIZE-1) south = board[row + 1][col];
		if(col > 0) west = board[row][col - 1];

		TigerTile nw = null, ne = null, se = null, sw = null;
		if (row > 0 && col > 0) nw = board[row-1][col-1];
		if (row > 0 && col < COLSIZE-1) ne = board[row-1][col+1];
		if (row < ROWSIZE-1 && col < COLSIZE-1) se = board[row+1][col+1];
		if (row < ROWSIZE-1 && col > 0) sw = board[row+1][col-1];

		//adjust CARTERSIAN coordinates based on given starting position
		int adjustedY = startY + (COLSIZE/2 - row);
		int adjustedX = startX + (col - ROWSIZE/2);

		//if the neighboring board tiles weren't null, add their locations to Moore
		if(north != null) mooreHood.add(new Location(adjustedX,adjustedY + 1));
		if(east != null) mooreHood.add(new Location(adjustedX + 1,adjustedY));
		if(south != null) mooreHood.add(new Location(adjustedX, adjustedY - 1));
		if(west != null) mooreHood.add(new Location(adjustedX - 1, adjustedY));
		if(nw != null) mooreHood.add(new Location(adjustedX - 1,adjustedY + 1));
		if(ne != null) mooreHood.add(new Location(adjustedX + 1,adjustedY + 1));
		if(se != null) mooreHood.add(new Location(adjustedX + 1, adjustedY - 1));
		if(sw != null) mooreHood.add(new Location(adjustedX - 1, adjustedY - 1));

		return mooreHood;
	}

	public boolean getPending() {
		return pending;
	}
	/**
	 *	getTile() calls on the TileStack's getTile() method to
	 *	obtain the specified tile type and its orientation.
	 *	Once obtained, it should be removed from the running list
	 * 	of available tiles in the TileStack.
	 *
	 *	TO DO: remove tile from running list of available tiles
	 *
	 *	@param type The corresponding tile type (A-Z, a), see Tile Types.png
	 *	@param orientation The desired orientation (0=0, 1=90, 2=180, 3=270)
	 *	@return the corresponding SquareTile
	 */
	//	public TigerTile getTile(String type, int orientation) {
	//		TigerTile result = tileStack.getTile(type, orientation);
	//		if (result == null) {
	//			setReason("Couldn't find specified tile. Try another.");
	//		}
	//		return result;
	//	}

	public TigerTile getTile(Location location) {
		return board[location.getY()][location.getX()];
	}

	public TigerTile getTile(int index) {
		TigerTile tile = tiles.get(index);
		//        tile.setOrientation(index);
		return tile;
	}

	public void setTileDeck(ArrayList<TigerTile> tiles) {
		this.tiles = tiles;
	}

	public int getStartX() {
		return BoardObject.startX;
	}

	public int getStartY() {
		return BoardObject.startY;
	}

	//MUTATORS

	public void setReason(String whyInvalid) {
		this.whyInvalid = whyInvalid;
	}

	/**
	 *	setAS() sets the array list of available spots to new spots
	 *	@param availableSpots an array list of new available spots to set
	 */
	public void setAS(ArrayList<Location> availableSpots) {
		this.availableSpots = availableSpots;
	}


	public void setIncomplete(Map<Integer, Region> incompleteRegions) {
		this.incompleteRegions = incompleteRegions;
	}

	public void setComplete(Set<Region> completedRegions) {
		this.completedRegions = completedRegions;
	}

	/**
	 *  valid() serves as our placement validity checker. It currently
	 *	checks to see if the location being placed is within bounds
	 *	of the board and if the location is in the available list of
	 *	spots.
	 *
	 * 	TO DO: check if the adjacent tiles at that location can accept the tile
	 *  @param tile the tile to be potentially placed
	 *	@param coord The Location on the board for the tile to be placed.
	 *	@return true if valid placement, false if not
	 */
	public boolean valid(TigerTile tile, Location coord) {

		if (pending) {
			setReason("Pending move still!");
			return false;
		}

		//check to see if there is any available tiles of the input type
		if (tile == null) return false;

		//get queried placement
		int row = coord.getY();
		int col = coord.getX();

		//if out of bounds of the board, or location filled return false automatically
		if ((row<0 || row>ROWSIZE-1) || (col<0 || col>COLSIZE-1)) {
			setReason("Out of board range. Resize the board?");
			return false;
		}

		if(board[row][col] != null) {
			setReason("Spot is filled. Try another location.");
			return false;
		}

		//find if the requested spot is in the list of accumulated available spots
		boolean found = false;
		int index = 0;
		for (int i = 0; i < availableSpots.size(); i++) {
			if (availableSpots.get(i).equals(coord)) {
				index = i;
				found = true;
				break;
			}
		}

		//if wasn't found in the list, return false
		if (!found) {
			setReason("Requested location isn't in the available spots list");
			return false;
		}

		//get adjacent tiles
		TigerTile north = null, east = null, south = null, west = null;
		if(row > 0) north = board[row - 1][col];
		if(col < COLSIZE-1) east = board[row][col + 1];
		if(row < ROWSIZE-1) south = board[row + 1][col];
		if(col > 0) west = board[row][col - 1];

		//if tile edges dont match north with adjacent touching edges, return false
		setReason("Can't place for given orientation.");
		if(north != null && north.getEdgeType(TileEdges.SOUTH) != tile.getEdgeType(TileEdges.NORTH))  return false;
		if(east != null && east.getEdgeType(TileEdges.WEST) != tile.getEdgeType(TileEdges.EAST)) return false;
		if(south != null && south.getEdgeType(TileEdges.NORTH) != tile.getEdgeType(TileEdges.SOUTH)) return false;
		if(west != null && west.getEdgeType(TileEdges.EAST) != tile.getEdgeType(TileEdges.WEST)) return false;
		setReason("");

		//else remove location from available spots list, return true
		availableSpots.remove(index);

		return true;
	}

	//Places a tile on the board at a given location
	public boolean place(TigerTile tile, Location coord) {

		int row = coord.getY();
		int col = coord.getX();
		int adjustedY = startY + (COLSIZE/2 - row);
		int adjustedX = startX + (col - ROWSIZE/2);
		String type = tile.getType();

		TigerTile north = null, east = null, south = null, west = null;
		if(row > 0) north = board[row - 1][col];
		if(col < COLSIZE-1) east = board[row][col + 1];
		if(row < ROWSIZE-1) south = board[row + 1][col];
		if(col > 0) west = board[row][col - 1];

		Location addnorth = null, addeast = null, addwest = null, addsouth = null;

		addnorth = new Location(adjustedX, adjustedY + 1);
		addeast = new Location(adjustedX + 1, adjustedY);
		addsouth = new Location(adjustedX, adjustedY - 1);
		addwest = new Location(adjustedX - 1, adjustedY);

		//remove potential dnorthlicate values (is there a better way to do this?)
		for (int i = 0; i < availableSpots.size(); i++) {
			if ((availableSpots.get(i).equals(addnorth)) || (availableSpots.get(i).equals(addwest)) || (availableSpots.get(i).equals(addeast)) || (availableSpots.get(i).equals(addsouth)) || availableSpots.get(i).equals(coord))
				availableSpots.remove(i);
		}

		boolean connectednorth = (north != null) ? true : false;
		boolean connectedeast = (east != null) ? true : false;
		boolean	connectedsouth = (south != null) ? true : false;
		boolean	connectedwest = (west != null) ? true : false;

		Terrain[] terrains = tile.getTerrains();
		for (Terrain terrain : terrains) {
			if (terrain instanceof LakeTerrain) {
				LakeRegion newRegion = new LakeRegion(terrain);
				int regionID = terrain.getRegionID();

				incompleteRegions.put(regionID,newRegion);
				allRegions.put(regionID, newRegion);
			}
			else if (terrain instanceof TrailTerrain) {
				TrailRegion newRegion = new TrailRegion(terrain);
				int regionID = terrain.getRegionID();

				incompleteRegions.put(regionID,newRegion);
				allRegions.put(regionID, newRegion);
			}
			else if (terrain instanceof JungleTerrain) {
				JungleRegion newRegion = new JungleRegion(terrain);
				int regionID = terrain.getRegionID();

				incompleteRegions.put(regionID,newRegion);
				allRegions.put(regionID, newRegion);
			}
		}

		if (connectedwest) { mergeTileRegions(west,tile,TileEdges.WEST); }
		if (connectedeast) { mergeTileRegions(east,tile,TileEdges.EAST); }
		if (connectednorth) { mergeTileRegions(north,tile,TileEdges.NORTH); }
		if (connectedsouth) { mergeTileRegions(south,tile,TileEdges.SOUTH); }


		//if adjacent tiles were empty, add them to available spots to plac
		if (north == null && addnorth != null) availableSpots.add(addnorth);
		if (east == null && addeast != null) availableSpots.add(addeast);
		if (south == null && addsouth != null) availableSpots.add(addsouth);
		if (west == null && addwest != null) availableSpots.add(addwest);

		if(tile.getCenter() == 'X') {
			DenRegion newDen = new DenRegion(coord);
			int regionID = newDen.getRegionID();
			incompleteRegions.put(regionID, newDen);
			allRegions.put(regionID, newDen);
			denRegions.add(newDen);
		}

		//set the tile's coordinate to it's new spot, place it, remove from stack
		tile.setCoord(coord);
		board[row][col] = tile;
		recentPlacement = coord;
		recentTile = tile;

		int index = 0;
		boolean found = false;
		for (int i = 0; i < availableSpots.size(); i++) {
			if (availableSpots.get(i).equals(coord)) {
				index = i;
				found = true;
				break;
			}
		}

		if (found) availableSpots.remove(index);

		return true;
	}



	//deprecated
	/**
	 *  place() will be what handles physically placing a specified tile at
	 *	the specified location. It calls on valid() to first determine if
	 *  the queried location is available for the tile. It also adds in the
	 *	the adjacent tiles to the available spots list if not already in there.
	 *
	 * 	TO DO: optimize, account for boundary conditions
	 *
	 *	@param tile The tile to be placed
	 *  @param coord The coordinate location to be placed at
	 *	@return true if placed, false if not
	 */
	public boolean placeTest(TigerTile tile, Location coord) {

		//proceed if valid placement/game is starting

		if (valid(tile,coord)) {
			
			place(tile, coord);
			confirm();
			
			return true;
		}

//		setReason("Wasn't able to place.");
		return false;
	}

	//Confirms the move that is to be played on the board
	public void confirm() {
		//update the dens and move any completed regions to the completed list
		updateDens();
		moveCompleted();
		//		switchPlayers(activePlayer);
		possibleTileSpots.clear();
		minSpots.clear();

		pending = false;
		tigerPlaced = false;
		crocodilePlaced = false;

		if (activePlayer.equals(players[0]))
		{
			activePlayer = players[1];
		}
		else {
			activePlayer = players[0];
		}

	}

	//	public void switchPlayers(Player player) {
	//		if (players[0].equals(player)) { activePlayer = players[1]; }
	//		else { activePlayer = players[0]; }
	//	}

	public Player getActivePlayer() {

		return activePlayer;

	}

	//updates active player
	public void switchToActivePlayer(Player player) {
		activePlayer = player;
	}

	//Determines if a tile is surrounded on all 8 sides
	public boolean isSurrounded(Location coord) {

		int row = coord.getY();
		int col = coord.getX();

		TigerTile tile = board[row-startY][col-startX];
		if (tile == null) return false;

		char special = tile.getSpecial();

		TigerTile north = null, east = null, south = null, west = null;
		TigerTile nw = null, ne = null, se = null, sw = null;

		if(row > 0) north = board[row - 1][col];
		if(col < COLSIZE-1) east = board[row][col + 1];
		if(row < ROWSIZE-1) south = board[row + 1][col];
		if(col > 0) west = board[row][col - 1];

		if(north == null || east == null || south == null || west == null) return false;

		if(special == 'X') {
			if (row > 0 && col > 0) nw = board[row-1][col-1];
			if (row > 0 && col < COLSIZE-1) ne = board[row-1][col+1];
			if (row < ROWSIZE-1 && col < COLSIZE-1) se = board[row+1][col+1];
			if (row < ROWSIZE-1 && col > 0) sw = board[row+1][col-1];
			if(nw == null || ne == null || se == null || sw == null) return false;
		}
		return true;
	}

	public void updateDens() {

		//find each den in the map of incomplete regions
		for (DenRegion denRegion : denRegions) { 

			//get the potential den and ID
			int denRegionID = denRegion.getRegionID();

			//if it was an actual Den Region
			if(denRegion instanceof DenRegion) {

				//obtain the Moore neighborhood and reset the Den Region's
				ArrayList<Location> newMoore = getMoore(((DenRegion) denRegion).getLocation());
				((DenRegion) denRegion).setMoore(newMoore);


				//now update every jungle that is inside of the den region's Moore neighborhood
				for (Location coord : newMoore) {

					//get the tile and its terrain (regions) based on the location given by the Moore neighborhood
					TigerTile temp = getTile(coord);
					Terrain[] tempTerrains = temp.getTerrains();

					//for every Jungle terrain found, add the den to the set of dens associated with it
					for (Terrain terrain : tempTerrains) {
						if(terrain instanceof JungleTerrain) {
							int jungleRegionID = terrain.getRegionID();
							Region jungleRegion = incompleteRegions.get(jungleRegionID);
							if (jungleRegion instanceof JungleRegion && jungleRegion != null) ((JungleRegion) jungleRegion).addDen(denRegionID);
						}
					}
				}
			}
		}
	}

	//function to move all of the completed regions and score in time accordingly
	public void moveCompleted() {


		Iterator it = incompleteRegions.entrySet().iterator();

		while(it.hasNext()) { 
			Map.Entry pair = (Map.Entry)it.next();
			Region region = (Region) pair.getValue();
			if(region.isCompleted() && !region.isScored()) {

				int score = region.getPotential();
				completedRegions.add(region);
				it.remove();
				region.setScored(true);

				//distribute scores
				int owner = regionOwner(region);
				if (owner != -1) adjustScore(owner, score);

				if (region.hasCrocodiles()) region.removeAllCrocodile();
				if (region.hasTigers()) region.removeAllTigers();

			}
		}
	}

	//Merges regions of two tiles across a given edge
	public void mergeTileRegions(TigerTile a, TigerTile b, int edge) {
		//tile A will serve as the parent, tile B will be the child

		TileEdges aEdges = a.getEdges();
		TileEdges bEdges = b.getEdges();
		Terrain aTop = null, aMid = null, aBot = null;
		Terrain bTop = null, bMid = null, bBot = null;

		switch (edge) {
		case TileEdges.NORTH:
			aTop = aEdges.getTerrain(TileEdges.SOUTHWEST);
			aMid = aEdges.getTerrain(TileEdges.SOUTH);
			aBot = aEdges.getTerrain(TileEdges.SOUTHEAST);
			bTop = bEdges.getTerrain(TileEdges.NORTHWEST);
			bMid = bEdges.getTerrain(TileEdges.NORTH);
			bBot = bEdges.getTerrain(TileEdges.NORTHEAST);
			break;
		case TileEdges.EAST:
			aTop = aEdges.getTerrain(TileEdges.NORTHWEST);
			aMid = aEdges.getTerrain(TileEdges.WEST);
			aBot = aEdges.getTerrain(TileEdges.SOUTHWEST);
			bTop = bEdges.getTerrain(TileEdges.NORTHEAST);
			bMid = bEdges.getTerrain(TileEdges.EAST);
			bBot = bEdges.getTerrain(TileEdges.SOUTHEAST);
			break;
		case TileEdges.SOUTH:
			aTop = aEdges.getTerrain(TileEdges.NORTHWEST);
			aMid = aEdges.getTerrain(TileEdges.NORTH);
			aBot = aEdges.getTerrain(TileEdges.NORTHEAST);
			bTop = bEdges.getTerrain(TileEdges.SOUTHWEST);
			bMid = bEdges.getTerrain(TileEdges.SOUTH);
			bBot = bEdges.getTerrain(TileEdges.SOUTHEAST);
			break;
		case TileEdges.WEST:
			aTop = aEdges.getTerrain(TileEdges.NORTHEAST);
			aMid = aEdges.getTerrain(TileEdges.EAST);
			aBot = aEdges.getTerrain(TileEdges.SOUTHEAST);
			bTop = bEdges.getTerrain(TileEdges.NORTHWEST);
			bMid = bEdges.getTerrain(TileEdges.WEST);
			bBot = bEdges.getTerrain(TileEdges.SOUTHWEST);
			break;
		default: break;
		}

		Region aRegion = allRegions.get(aMid.getRegionID());
		Region bRegion = allRegions.get(bMid.getRegionID());
		ArrayList<Integer> tileConnections = bMid.getTileConnections();
		ArrayList<TigerObject> tigers;
		ArrayList<CrocodileObject> crocodiles = null;
		Iterator<TigerObject> it = null;
		Iterator<CrocodileObject> itCrocs = null;

		int oldRegionID;

		if(aRegion != null && bRegion != null && aRegion.getRegionID() != bRegion.getRegionID()) {
			tigers = bRegion.getTigers();
			crocodiles = bRegion.getCrocodiles();
			it = tigers.iterator();
			itCrocs = crocodiles.iterator();
			oldRegionID = bRegion.getRegionID();

			for (Integer entry : tileConnections) bEdges.setEdge(entry, aRegion.getRegionID());

			aRegion.addTerrain(bRegion.getTerrains(),aRegion.getRegionID());

			if (aRegion.hasTigers()) while(it.hasNext()) { aRegion.addTiger(bRegion.removeTiger()); }
			if (aRegion.hasCrocodiles()) while(itCrocs.hasNext()) { aRegion.addCrocodile(bRegion.removeCrocodile()); }

			updateMin(aRegion.getRegionID(),aRegion.getRecentMin());
			incompleteRegions.remove(oldRegionID);
		}

		//if we are connecting a trail, then we have to check the top and bottom to connect jungles as well
		if (bMid.getType() == 'T') {

			//top
			aRegion = allRegions.get(aTop.getRegionID());
			bRegion = allRegions.get(bTop.getRegionID());

			if(aRegion != null && bRegion != null && aRegion.getRegionID() != bRegion.getRegionID()) {
				tigers = bRegion.getTigers();
				if(tigers != null) it = tigers.iterator();
				if (crocodiles != null) itCrocs = crocodiles.iterator();

				oldRegionID = bRegion.getRegionID();

				tileConnections = bTop.getTileConnections();
				for (Integer entry : tileConnections) bEdges.setEdge(entry, aRegion.getRegionID());

				aRegion.addTerrain(bRegion.getTerrains(),aRegion.getRegionID());

				if (aRegion.hasTigers()) while(it.hasNext()) { aRegion.addTiger(bRegion.removeTiger()); }
				if (aRegion.hasCrocodiles()) while(itCrocs.hasNext()) { aRegion.addCrocodile(bRegion.removeCrocodile()); }

				updateMin(aRegion.getRegionID(),aRegion.getRecentMin());
				incompleteRegions.remove(oldRegionID);
			}

			//bottom
			aRegion = allRegions.get(aBot.getRegionID());
			bRegion = allRegions.get(bBot.getRegionID());

			if(aRegion != null && bRegion != null && aRegion.getRegionID() != bRegion.getRegionID()) {
				tigers = bRegion.getTigers();
				if(tigers != null) it = tigers.iterator();
				if (crocodiles != null) itCrocs = crocodiles.iterator();

				oldRegionID = bRegion.getRegionID();

				tileConnections = bBot.getTileConnections();
				for (Integer entry : tileConnections) bEdges.setEdge(entry, aRegion.getRegionID());

				aRegion.addTerrain(bRegion.getTerrains(),aRegion.getRegionID());

				if (aRegion.hasTigers()) while(it.hasNext()) { aRegion.addTiger(bRegion.removeTiger()); }
				if (aRegion.hasCrocodiles()) while(itCrocs.hasNext()) { aRegion.addCrocodile(bRegion.removeCrocodile()); }

				updateMin(aRegion.getRegionID(),aRegion.getRecentMin());
				incompleteRegions.remove(oldRegionID);
			}
		}		
	}

	public void updateMin(int regionID, int value) {

		if (minSpots.containsKey(regionID)) {
			if (value < minSpots.get(regionID)) {
				minSpots.remove(regionID);
				minSpots.put(regionID, value);
			}
		} else minSpots.put(regionID,  value);

	}

	public int adjustIndex(int index, boolean reverse) {

		if (!reverse) {
			if(index == 0) return 1;
			else if (index == 1) return 2;
			else if (index == 2) return 3;
			else if (index == 3) return 6;
			else if (index == 4) return 9;
			else if (index == 5) return 8;
			else if (index == 6) return 7;
			else if (index == 7) return 4;
			else return index;
		}
		else {
			if(index == 1) return 0;
			else if (index == 2) return 1;
			else if (index == 3) return 2;
			else if (index == 4) return 7;
			else if (index == 6) return 3;
			else if (index == 7) return 6;
			else if (index == 8) return 5;
			else if (index == 9) return 4;
			else return index;
		}
	}

	public boolean removeTiger(Location location) {

		TigerTile tile = getTile(location);
		TigerObject stray = tile.getTiger();
		int regionID = stray.getRegionID();
		Player owner = stray.getTigerOwner();
		Region region = allRegions.get(regionID);

		ArrayList<TigerObject> tigers = region.getTigers();

		for (int i = 0; i < tigers.size(); i++) {
			if (tigers.get(i).getTigerOwner().getID().equals(owner.getID())) {
				region.removeTiger(i);
				break;
			}
		}

		tile.removeTiger();
		return true;
	}

	public boolean placeTiger(Location location) {

		TigerTile tile = getTile(location);
		int regionID = tile.getTiger().getRegionID();
		Region region = allRegions.get(regionID);
		TigerObject stray = activePlayer.removeTiger();
		stray.setRegionID(regionID);
		stray.setLocation(location);
		region.addTiger(stray);
		tile.addTiger(stray);
		return true;

	}

	public boolean placeTiger(int index) {

		for (Terrain terrain : recentTile.getTerrains()) { 
			if (!minSpots.containsKey(terrain.getRegionID())) { 
				minSpots.put(terrain.getRegionID(), terrain.getZoneMin());
			}
		}

		if (index > 9 || index < 1) {
			possibleTileSpots.clear();
			setReason("Index out of bounds");
			return false;
		}

		if (!activePlayer.hasTigers()) {
			possibleTileSpots.clear();

			setReason("Player doesn't have any tigers!");
			return false;
		}

		TigerTile last = recentTile;
		if (last == null) {
			possibleTileSpots.clear();

			setReason("Error: no placed tile?");
			return false;
		}


		if (tigerPlaced) {
			possibleTileSpots.clear();

			setReason("Already placed a Tiger!");
			return false;
		}

		int terrainPoint = adjustIndex(index, true);
		Terrain terrain = last.getEdge(terrainPoint);
		int regionID = terrain.getRegionID();

		Region region; 
		if (index == 5 && !denRegions.isEmpty()) region = denRegions.get(denRegions.size()-1);
		else region = incompleteRegions.get(regionID);

		if (region == null) {
			for (Region reg : completedRegions) {
				if (reg.getRegionID() == regionID) region = reg;
			}
		}

		if(region == null) {
			possibleTileSpots.clear();

			setReason("ERROR: Region wasn't found.");
			return false;
		}

		if (index == 5 && last.getCenter() != 'X') {
			possibleTileSpots.clear();

			setReason("This isn't a den!");
			return false;
		}

		if(region.hasTigers()) {
			possibleTileSpots.clear();

			setReason("Region at index already has a Tiger!");
			return false;
		}

		int min = index;
		if (minSpots.containsKey(regionID)) {
			min = region.getRecentMin();
		}
		if(min != index && index != 5) {
			possibleTileSpots.clear();
//			System.out.println(min);
			setReason("Specified index was not the minimum");
			return false;
		}

		TigerObject stray = activePlayer.removeTiger();
		stray.setRegionID(regionID);
		stray.setLocation(recentPlacement);
		region.addTiger(stray);
		recentTile.addTiger(stray);
		tigerPlaced = true;

		System.out.println(region);

		return true;
	}

	public boolean placeCrocodile() {

		Location recent = recentPlacement;
		TigerTile last = getTile(recent);
		if (last == null) {
			setReason("Error: no placed tile?");
			return false;
		}

		if (!activePlayer.hasCrocs()) {
			setReason("Player doesn't have any tigers!");
			return false;
		}

		if (crocodilePlaced) {
			setReason("Already placed a Crocodile!");
			return false;
		}

		Terrain[] terrains = last.getTerrains();
		CrocodileObject hatchling = activePlayer.removeCroc();

		for (Terrain terrain : terrains) {
			int regionID = 0;
			if (terrain instanceof LakeTerrain || terrain instanceof TrailTerrain) {
				regionID = terrain.getRegionID();

				Region region = allRegions.get(regionID);

				if(region == null) {
					break;
				}
				if (!region.hasCrocodiles()) {
					region.addCrocodile(hatchling);
				}
			}
		}

		crocodilePlaced = true;
		return true;
	}


	public void setPending(boolean pending) {
		pending = pending;
	}

	public void setStart(int x, int y) {
		startX = x;
		startY = y;
	}

	/**
	 *	setPlayers() is self explanatory - set the current Player[]
	 *	to a new Player[]
	 */
	public void setPlayers(Player[] players) {
		this.players = players;
		activePlayer = players[0];
	}

	/**
	 *	setBoard() is self explanatory. It sets the current board to a new board.
	 */
	public void setBoard(TigerTile[][] board) {
		this.board = board;
	}

	/**
	 * start() begins the game by placing the starting tile based on the specified
	 * starting location and orientation. Every specified location from there is
	 * therefore relative to it.
	 */
	public void start(TigerTile startTile, int startX, int startY) {
		setStart(startX, startY);
		availableSpots.add(new Location(startX,startY));
		place(startTile, new Location(startX,startY));
		activePlayer = players[0];

		pending = false;
	}

	public void end() {
		//end game logic and scoring

		for(Map.Entry<Integer, Region> entry : incompleteRegions.entrySet()) {
			Region region = entry.getValue();

			if (region instanceof JungleRegion) {

				//get the adjacent lakes and dens along the jungle region
				Set<Integer> dens = ((JungleRegion) region).getDens();
				Set<Integer> lakes = ((JungleRegion) region).getLakes();
				int completeDens = 0;
				int completeLakes = 0;

				//get the number of completed dens
				for (Integer den : dens) {
					Region denRegion = allRegions.get(den);
					if (denRegion.isCompleted()) {
						completeDens++;
					}
				}

				//get the number of completed lakes
				for (Integer lake : lakes) {
					Region lakeRegion = allRegions.get(lake);
					if (lakeRegion.isCompleted()) {
						completeLakes++;
					}
				}

				//distribute scores for jungles
				int score = (3 * completeLakes) + (5 * completeDens);
				int owner = regionOwner(region);
				if (owner != -1) adjustScore(owner, score);

			}
			else if (region instanceof TrailRegion) {

				//distribute scores for incomplete trails
				int score = ((TrailRegion)region).getPotential();
				int owner = regionOwner(region);
				if (owner != -1) adjustScore(owner, score);

			}
			else if (region instanceof LakeRegion) {

				//distribute scores for incomplete lakes
				int score = ((LakeRegion)region).getPotential();
				int owner = regionOwner(region);
				if (owner != -1) adjustScore(owner, score);
			}
		}

		for (DenRegion den : denRegions) { 
			int score = den.getPotential();
			int owner = regionOwner(den);
			if (owner != -1) adjustScore(owner, score);			
		}
		printScores();
	}

	//Determines owner of particular region
	//Based upon number of tigers in region and the owners of the tigers in that region
	public int regionOwner(Region region) {
		ArrayList<TigerObject> tigers = region.getTigers();
		int player1count = 0;
		int player2count = 0;

		//determine who had more tigers
		for (TigerObject tiger : tigers) {
			if(tiger.getTigerOwner().equals(players[0])) { player1count++; }
			else { player2count++; }
		}

		if(player1count != 0 && player2count != 0 && player1count == player2count) {
			return 2;
		}
		else if (player1count > player2count) { return 0; }
		else if (player2count > player1count) { return 1; }
		return -1;
	}

	//Determines if the next tile to be played has the ability to placed
	//Used by AI to make gameplay decisions
	public boolean canPlace(TigerTile tile) {

		possibleTileSpots.clear();
		ArrayList<Location> spots = availableSpots;
		TileEdges edges = tile.getEdges();
		Terrain[] terrains = edges.getTerrains();

		char[] edgeTypes0 = {terrains[0].getType(),terrains[1].getType(),terrains[2].getType(),terrains[3].getType(),terrains[4].getType(),terrains[5].getType(),terrains[6].getType(),terrains[7].getType(),};
		char[] edgeTypes90 = {terrains[2].getType(),terrains[3].getType(),terrains[4].getType(),terrains[5].getType(),terrains[6].getType(),terrains[7].getType(),terrains[0].getType(),terrains[1].getType(),};
		char[] edgeTypes180 = {terrains[4].getType(),terrains[5].getType(),terrains[6].getType(),terrains[7].getType(),terrains[0].getType(),terrains[1].getType(),terrains[2].getType(),terrains[3].getType(),};
		char[] edgeTypes270 = {terrains[6].getType(),terrains[7].getType(),terrains[0].getType(),terrains[1].getType(),terrains[2].getType(),terrains[3].getType(),terrains[4].getType(),terrains[5].getType(),};

		boolean canPlace = false;

		for (Location spot : spots) {

			TigerTile north = null, east = null, south = null, west = null;

			int row = spot.getY();
			int col = spot.getX();

			if (row > 0) north = board[row - 1][col];
			if (col < COLSIZE-1) east = board[row][col + 1];
			if (row < ROWSIZE-1) south = board[row + 1][col];
			if (col > 0) west = board[row][col - 1];

			boolean connectednorth = (north != null) ? true : false,
					connectedeast = (east != null) ? true : false,
							connectedsouth = (south != null) ? true : false,
									connectedwest = (west != null) ? true : false;

			Set<Integer> compare = new LinkedHashSet<Integer>();

			char[] edgeCompare = new char[8];
			if (connectednorth) {
				edgeCompare[1] = north.getEdgeType(5);
				compare.add(1);
			}
			if (connectedeast) {
				edgeCompare[3] = east.getEdgeType(7);
				compare.add(3);
			}
			if (connectedsouth) {
				edgeCompare[5] = south.getEdgeType(1);
				compare.add(5);
			}
			if (connectedwest) {
				edgeCompare[7] = west.getEdgeType(3);
				compare.add(7);
			}

			boolean canPlace0 = true, canPlace90 = true, canPlace180 = true, canPlace270 = true;

			//check 0
			for (Integer integer : compare) {
				if (edgeCompare[integer] != edgeTypes0[integer]) {
					canPlace0 = false;
					break;
				}
			}

			//check 90
			for (Integer integer : compare) {
				if (edgeCompare[integer] != edgeTypes90[integer]) {
					canPlace90 = false;
					break;
				}
			}

			//check 180
			for (Integer integer : compare) {
				if (edgeCompare[integer] != edgeTypes180[integer]) {
					canPlace180 = false;
					break;
				}
			}

			//check 270
			for (Integer integer : compare) {
				if (edgeCompare[integer] != edgeTypes270[integer]) {
					canPlace270 = false;
					break;
				}
			}

			if (canPlace0) {
				possibleTileSpots.add(new TilePair(spot, 0));
			}

			if (canPlace90) {
				possibleTileSpots.add(new TilePair(spot, 90));
			}

			if (canPlace180) {
				possibleTileSpots.add(new TilePair(spot, 180));
			}

			if (canPlace270) {
				possibleTileSpots.add(new TilePair(spot, 270));
			}

			if (canPlace0 || canPlace90 || canPlace180 || canPlace270) {
				canPlace = true;
			}

		}

		//		System.out.print(possibleTileSpots);
		return canPlace;
	}


	public void adjustScore(int index, int score) {
		if (index == 2) {
			players[0].addScore(score);
			players[1].addScore(score);
		}
		else {
			players[index].addScore(score);
		}
	}

	public void printScores() {
		System.out.println(players[0]);
		System.out.println(players[1]);
	}

	/**
	 *	Print out the game board in a naive format. Shows locations as their
	 *	coordinates or as the tile that is currently placed.
	 */
	public void print() {
		for (int row = 0; row < ROWSIZE; row++) {
			for (int col = 0; col < COLSIZE; col++) {
				if(board[row][col] == null) System.out.print("(" + ((col - COLSIZE/2) + startY) + "," + ((ROWSIZE/2 - row) + startX) + ")\t");
				else System.out.print(board[row][col].getType() + "\t");
			}
			System.out.println("\n");
		}
	}

	/**
	 *	Print out the current running list of available spots in naive format.
	 *  If empty list, print none
	 */
	public void printSpots() {
		System.out.println("Available spots (" + availableSpots.size() + "):");
		if (availableSpots.isEmpty()) System.out.println("None");
		else
			for (int i = 0; i < availableSpots.size(); i++) {
				System.out.print(availableSpots.get(i).toString() + " ");
			}//end for
	}//end printSpots
}
