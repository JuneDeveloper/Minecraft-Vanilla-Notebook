package com.jwg.coord_book;

import com.jwg.coord_book.util.ensureFileStructureExists;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class CoordBook implements ModInitializer {
	public static final boolean developerMode = false;
	public static final String version = "0.2.0";
	public static final String project = "Coordinate-Book";
	public static final Logger LOGGER = LoggerFactory.getLogger(project);


	public static final Identifier BOOK_ICON = new Identifier("coordbook:textures/gui/book.png");

	@Override
	public void onInitialize() {
		LOGGER.info("{} has started initializing!", project);
		ensureFileStructureExists.createFiles(ensureFileStructureExists.exists("CoordinateBook/"));
		File firstPage = new File("CoordinateBook/0.json");
		try {
			if (firstPage.createNewFile()){
				LOGGER.info("Created first page of the coordinate book");
			}
			else{
				LOGGER.info("Unable to create the first page of the coordinate book; it might already exist");
				//TODO: Read & display page
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		LOGGER.info("{} has finished initializing!", project);
	}
}
