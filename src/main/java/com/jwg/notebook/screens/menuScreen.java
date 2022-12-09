package com.jwg.notebook.screens;

import com.jwg.notebook.Notebook;
import com.jwg.notebook.gui.button.gotobookmark;
import com.jwg.notebook.gui.sidebar;
import com.jwg.notebook.util.addCharacter;
import com.jwg.notebook.util.readPage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import static com.jwg.notebook.Notebook.*;

@Environment(EnvType.CLIENT)
public class menuScreen extends Screen {

    public static boolean global = true;
    public static int page = 0;
    public static int bookmarkedpage = 0;
    public static int pageLimit = -1;
    public static final Identifier GLOBAL_ICON = new Identifier("textures/gui/globe.png");
    public static final Identifier BOOK_TEXTURE = new Identifier("textures/gui/book.png");
    public static final Identifier BOOK_SIDEBAR_TEXTURE = new Identifier("notebook:textures/gui/sidebar.png");
    private List<OrderedText> cachedPage;
    private Text pageIndexText;
    private final boolean pageTurnSound;
    private String versionText;
    private String contents;

    private int cursorLoc;
    public static TexturedButtonWidget delete;
    public static TexturedButtonWidget bookmark;
    public static TexturedButtonWidget bookmarkPgB;
    public menuScreen() {
        this(true);
    }

    char ltchr;

    private menuScreen(boolean bl) {
        super(NarratorManager.EMPTY);
        this.contents = "";
        this.versionText = "";
        this.cachedPage = Collections.emptyList();
        this.pageIndexText = ScreenTexts.EMPTY;
        this.pageTurnSound = bl;
        this.ltchr = 0;
        this.cursorLoc = 0;
        pageLimit = pageLimit - 1;
    }

    protected void init() {
        assert this.client != null;
        this.client.keyboard.setRepeatEvents(true);
        this.addButtons();
    }

    protected void addButtons() {
        //Done button
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, 196, 200, 20, ScreenTexts.DONE, (button) -> { assert this.client != null; this.client.setScreen(null); }));

        //Sidebar buttons
        this.addDrawableChild(delete = sidebar.addSidebarButton(0, DELETE_ICON, this, "delete", 8, 8, (button -> com.jwg.notebook.gui.button.delete.onPress(page))));
        this.addDrawableChild(bookmark = sidebar.addSidebarButton(1, BOOKMARK_MARKER_ICON, this, "bookmark", 8, 8, (button -> com.jwg.notebook.gui.button.gotobookmark.onPress())));
        this.addDrawableChild(bookmarkPgB = sidebar.addSidebarButton(2, BOOKMARK_ICON, this, "bookmarkb", 8, 8, (button -> com.jwg.notebook.gui.button.bookmark.onPress())));

        assert this.client != null;

        //Page buttons (arrows)
        int i = (this.width - 192) / 2;
        this.addDrawableChild(new PageTurnWidget(i + 116, 159, true, (button) -> {
            if (page != pageLimit || pageLimit < 0) { if (page >= pageLimit && pageLimit > 0) { page = pageLimit; }this.goToNextPage(); assert this.client != null; this.client.setScreen(this); }
        }, this.pageTurnSound));
        this.addDrawableChild(new PageTurnWidget(i + 43, 159, false, (button) -> {
            goToPreviousPage();
            assert this.client != null;
            this.client.setScreen(this);
        }, this.pageTurnSound));

        if (developerMode) { this.versionText = "Vanilla Notebook "+version+" Developer build";
        } else { this.versionText = "Vanilla Notebook " + version; }
    }
    public static void goToPreviousPage() {
        --page;
        if (page <= -1) {
            page = 0;
        }
    }
    protected void goToNextPage() {
        ++page;
        if (!new File(pageLocation+"/"+page+".jdat").exists()) {
            try {
                if (new File(pageLocation+"/"+page+".jdat").createNewFile()) {
                    Notebook.LOGGER.info("page {} has been created", page);
                }
            } catch (IOException e) {
                Notebook.LOGGER.error("page {} is unable to be created", page);
                throw new RuntimeException(e);
            }
        }
    }
    public void renderBookText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BOOK_TEXTURE);
        int i = (this.width - 192) / 2;

        this.drawTexture(matrices, i, 2, 0, 0, 192, 192);
        this.pageIndexText = Text.translatable("book.pageIndicator", page + 1, Math.max((Objects.requireNonNull(new File(pageLocation+"/").list()).length), 1));

        drawStringWithShadow(matrices, this.textRenderer, String.valueOf(versionText), 2, this.height - 10, 16777215);

        StringBuilder fulldata = new StringBuilder();
        try {
            Scanner readPageContent = new Scanner(new File(pageLocation+"/"+page+".jdat"));
            while (readPageContent.hasNextLine()) {
                String data = readPageContent.nextLine();
                fulldata.append(data);
            }
            readPageContent.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.contents = addCharacter.add(String.valueOf(fulldata), cursorLoc, "|");
        StringVisitable stringVisitable = StringVisitable.plain(this.contents);

        this.cachedPage = this.textRenderer.wrapLines(stringVisitable, 114);


        int k = this.textRenderer.getWidth(this.pageIndexText);
        this.textRenderer.draw(matrices, this.pageIndexText, (float)(i - k + 192 - 44), 18.0F, 1);

        Objects.requireNonNull(this.textRenderer);
        int l = Math.min(128 / 9, this.cachedPage.size());



        for(int m = 0; m < l; ++m) {
            OrderedText orderedText = this.cachedPage.get(m);
            TextRenderer var10000 = this.textRenderer;
            float var10003 = (float)(i + 36);
            Objects.requireNonNull(this.textRenderer);
            var10000.draw(matrices, orderedText, var10003, (float)(32 + m * 9), 0);

        }
        super.render(matrices, mouseX, mouseY, delta);
    }
    public void renderSideBar(MatrixStack matrices, int mouseX, int mouseY, float delta){
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BOOK_SIDEBAR_TEXTURE);
        int i = (this.width - -150) / 2;

        this.drawTexture(matrices, i, 2, 0, 0, 36, 180);
        super.render(matrices, mouseX, mouseY, delta);
    }
    public void renderTooltips(MatrixStack matrices){
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BOOK_SIDEBAR_TEXTURE);

        if (delete.isHovered()) {
            drawTextWithShadow(matrices, this.textRenderer, Text.translatable("tooltip.book.delete"), this.width/2 +115, 7+12, 16777215);
        }
        else if (bookmark.isHovered()) {
            drawTextWithShadow(matrices, this.textRenderer, Text.translatable("tooltip.book.bookmark"), this.width/2 +115, 7+(12*2), 16777215);
        }
        else if (bookmarkPgB.isHovered()) {
            drawTextWithShadow(matrices, this.textRenderer, Text.translatable("tooltip.book.bookmarkb"), this.width/2 +115, 7+(12*3), 16777215);
        }


    }
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        renderBookText(matrices, mouseX, mouseY, delta);
        renderSideBar(matrices, mouseX, mouseY, delta);
        renderTooltips(matrices);
    }

    public boolean handleTextClick(Style style) {
        assert style != null;
        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null) {
            return false;
        } else if (clickEvent.getAction() == ClickEvent.Action.CHANGE_PAGE) {
            String string = clickEvent.getValue();

            try {
                int i = Integer.parseInt(string) - 1;
                return this.goToPage(i);
            } catch (Exception var5) {
                return false;
            }
        } else {
            boolean bl = super.handleTextClick(style);
            if (bl && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
                this.closeScreen();
            }

            return bl;
        }
    }
    private boolean goToPage(int i) {
        page = i;
        assert this.client != null;
        this.client.setScreen(this);
        return true;
    }
    public void closeScreen() {
        assert this.client != null;
        this.client.setScreen(null);
    }
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Style style = this.getTextStyleAt(mouseX, mouseY);
            if (style != null && this.handleTextClick(style)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    public boolean charTyped(char chr, int modifiers) {

            this.ltchr = chr;
            String keystring = String.valueOf(this.ltchr);
            StringBuilder fulldata = readPage.read(pageLocation, page);

            fulldata = new StringBuilder(addCharacter.add(String.valueOf(fulldata), cursorLoc, keystring));

            try {
                FileWriter updatePage = new FileWriter(pageLocation+"/"+page+".jdat");
                updatePage.write(String.valueOf(fulldata));
                updatePage.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Objects.requireNonNull(this.client).setScreen(this);

        return true;

    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        System.out.println(keyCode + " + " + scanCode + " + " + modifiers);
        //Backspace/Delete
        if (keyCode == 259) {
            StringBuilder fulldata = new StringBuilder();
            Scanner readFile = null;
            try { readFile = new Scanner(new File(pageLocation+"/"+page+".jdat"));
            } catch (FileNotFoundException e) { throw new RuntimeException(e); }
            while (readFile.hasNextLine()) {
                String data = readFile.nextLine();
                fulldata.append(data);
                System.out.println(data);
            }
            readFile.close();
            try {
                FileWriter updatePage = new FileWriter(pageLocation+"/"+page+".jdat");
                if (fulldata.length()-1-cursorLoc >= 0) updatePage.write(String.valueOf(fulldata.deleteCharAt(fulldata.length()-1-cursorLoc)));
                updatePage.close();
            } catch (IOException e) { e.printStackTrace(); }
        }
        if (keyCode == 261) {
            StringBuilder fulldata = readPage.read(pageLocation, page);
            try {
                FileWriter updatePage = new FileWriter(pageLocation+"/"+page+".jdat");
                if (fulldata.length()-cursorLoc != fulldata.length()) {
                    if (fulldata.length()-1-cursorLoc >= 0) updatePage.write(String.valueOf(fulldata.deleteCharAt(fulldata.length()-cursorLoc)));
                    cursorLoc--;
                } else updatePage.write(String.valueOf(fulldata));
                updatePage.close();
            } catch (IOException e) { e.printStackTrace(); }

        }

        int location = cursorLoc;
        //Right/left arrows
        if (keyCode == 262 && location++ > 0) cursorLoc--;
        if (keyCode == 263 && location-- < this.contents.length()-1) cursorLoc++;

        //Escape
        if (keyCode == 256) {
            assert this.client != null;
            this.client.setScreen(null);
        }

        if (keyCode == 257 && scanCode == 28) {

            String filedat = readPage.read(pageLocation, page) + "\nW";
            try {
                FileWriter f = new FileWriter(pageLocation+"/"+page+".jdat");
                f.write(filedat);
                f.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return true;
    }
    @Nullable
    public Style getTextStyleAt(double x, double y) {
        if (!this.cachedPage.isEmpty()) {
            int i = MathHelper.floor(x - (double) ((this.width - 192) / 2) - 36.0);
            int j = MathHelper.floor(y - 2.0 - 30.0);
            if (i >= 0 && j >= 0) {
                Objects.requireNonNull(this.textRenderer);
                int k = Math.min(128 / 9, this.cachedPage.size());
                if (i <= 114) {
                    assert this.client != null;
                    Objects.requireNonNull(this.client.textRenderer);
                    if (j < 9 * k + k) {
                        Objects.requireNonNull(this.client.textRenderer);
                        int l = j / 9;
                        if (l < this.cachedPage.size()) {
                            OrderedText orderedText = this.cachedPage.get(l);
                            return this.client.textRenderer.getTextHandler().getStyleAt(orderedText, i);
                        }

                        return null;
                    }
                }

            }
        }
        return null;
    }

}