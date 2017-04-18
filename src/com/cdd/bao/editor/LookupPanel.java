/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2016 Collaborative Drug Discovery Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2.0
 * as published by the Free Software Foundation:
 * 
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.cdd.bao.editor;

import com.cdd.bao.*;
import com.cdd.bao.template.*;
import com.cdd.bao.util.Lineup;

import java.io.*;
import java.util.*;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.geometry.*;
import javafx.util.*;

/*
	Lookup panel: takes a partially specified schema value and opens up the vocabulary list, to make it easy to pick URI/label/description
	combinations.
*/

public class LookupPanel extends Dialog<LookupPanel.Resource[]>
{
	private Vocabulary vocab = null;
	private Vocabulary.Hierarchy hier = null;
	private boolean isProperty; // false = value lookup, true = property lookup
	private Set<String> usedURI, exclURI;
	private boolean multi;

	public static final class Resource
	{
		public String uri, label, descr;
		public boolean beingUsed;
		
		public Resource(String uri, String label, String descr)
		{
			this.uri = uri;
			this.label = label == null ? "" : label;
			this.descr = descr == null ? "" : descr;
		}
	};
	private List<Resource> resources = new ArrayList<>();

	private TabPane tabber = new TabPane();
	private Tab tabList = new Tab("List"), tabTree = new Tab("Hierarchy");

	private TextField fieldSearch = new TextField();
	private TableView<Resource> tableList = new TableView<>();

    private TreeItem<Vocabulary.Branch> treeRoot = new TreeItem<>(new Vocabulary.Branch(null, null));
    private TreeView<Vocabulary.Branch> treeView = new TreeView<>(treeRoot);

    private final class HierarchyTreeCell extends TreeCell<Vocabulary.Branch>
    {
        public void updateItem(Vocabulary.Branch branch, boolean empty)
        {
            super.updateItem(branch, empty);
            
            if (branch != null)
            {
                String text = "URI <" + branch.uri + ">";
    			String descr = vocab.getDescr(branch.uri);
                if (descr != null && descr.length() > 0) text += "\n\n" + descr;
                Tooltip tip = new Tooltip(text);
                tip.setWrapText(true);
                tip.setMaxWidth(400);
    			Tooltip.install(this, tip);
            }
     
            if (empty)
            {
                setText(null);
                setGraphic(null);
            }
            else 
            {
            	String label = branch.label;

    			String style = "-fx-font-family: arial; -fx-text-fill: black; -fx-font-weight: normal;";
    			if (usedURI.contains(branch.uri)) style = "-fx-text-fill: #000080; -fx-font-weight: bold;";
    			else if (exclURI.contains(branch.uri)) style = "-fx-text-fill: #800080; -fx-font-weight: bold;";
    			
    			if (branch.uri.startsWith(ModelSchema.PFX_BAO) || branch.uri.startsWith(ModelSchema.PFX_BAT)) 
    			{
    				style += " -fx-font-style: normal;";
    			}
    			else 
    			{
    				style += " -fx-font-style: italic;";
    				label += " *";
    			}
            	
            	setText(label);
   				setStyle(style);
                setGraphic(getTreeItem().getGraphic());
    	    }
    	    
        }
    }

    private final int PADDING = 2;
       
	// ------------ public methods ------------

	public LookupPanel(boolean isProperty, String searchText, Set<String> usedURI, Set<String> exclURI, boolean multi)
	{
		super();
		
		this.isProperty = isProperty;
		this.usedURI = usedURI;
		this.exclURI = exclURI;
		this.multi = multi;
		
		loadResources();
		
		setTitle("Lookup " + (isProperty ? "Property" : multi ? "Values" : "Value"));

		setResizable(true);

		for (Tab tab : new Tab[]{tabList, tabTree}) {tab.setClosable(false);}

		setupList(searchText);
		setupTree(searchText);

		tabber.getTabs().addAll(tabList, tabTree);

		getDialogPane().setContent(tabber);

		getDialogPane().getButtonTypes().add(new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
		
		// setup the buttons
		
		ButtonType btnTypeUse = new ButtonType("Use", ButtonBar.ButtonData.OK_DONE);
		getDialogPane().getButtonTypes().add(btnTypeUse);
		setResultConverter(buttonType ->
		{
			if (buttonType == btnTypeUse) return composeCurrentValue();
			return null;
		});
		Button btnUse = (Button)getDialogPane().lookupButton(btnTypeUse);
		btnUse.addEventFilter(ActionEvent.ACTION, event ->
		{
			if (tableList.getSelectionModel().getSelectedIndex() < 0 &&
				treeView.getSelectionModel().getSelectedIndex() < 0) event.consume();
		});
		tableList.setOnMousePressed(event ->
		{
			if (event.isPrimaryButtonDown() && event.getClickCount() == 2) btnUse.fire();
		});
		tableList.setOnKeyPressed(event ->
		{
			if (event.getCode() == KeyCode.ENTER) btnUse.fire();
		});
		
        tableList.getSelectionModel().setSelectionMode(multi ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        treeView.getSelectionModel().setSelectionMode(multi ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);

		tabber.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
		{
			if (oldValue == tabList && newValue == tabTree) syncSelectionListToTree();
			else if (oldValue == tabTree && newValue == tabList) syncSelectionTreeToList();
		});
		
        Platform.runLater(() -> fieldSearch.requestFocus());
	}
	
	// if there's a starting value, set it in the list
	public void setInitialURI(String uri)
	{
		if (uri == null || uri.length() == 0) return;
		List<Resource> resList = tableList.getItems();
		for (int n = 0; n < resList.size(); n++) if (resList.get(n).uri.equals(uri))
		{
			tableList.getSelectionModel().clearAndSelect(n);
			return;
		}
	}
	
	// ------------ private methods ------------

	private void loadResources()
	{
		vocab = Vocabulary.globalInstance();
		hier = isProperty ? vocab.getPropertyHierarchy() : vocab.getValueHierarchy();
		
		String[] source = isProperty ? vocab.getPropertyURIs() : vocab.getValueURIs();
		for (String uri : source)
		{
			Resource res = new Resource(uri, vocab.getLabel(uri), vocab.getDescr(uri));
			res.beingUsed = usedURI.contains(uri);
			resources.add(res);
		}
	}

	private void setupList(String searchText)
	{
		Lineup line = new Lineup(PADDING);
		line.add(fieldSearch, "Search:", 1, 0);
 
        tableList.setEditable(false);
 
        TableColumn<Resource, String> colUsed = new TableColumn<>("U");
		colUsed.setMinWidth(20);
		colUsed.setPrefWidth(20);
        colUsed.setCellValueFactory(resource -> {return new SimpleStringProperty(resource.getValue().beingUsed ? "\u2713" : "");});

        TableColumn<Resource, String> colURI = new TableColumn<>("URI");
		colURI.setMinWidth(150);
        colURI.setCellValueFactory(resource -> {return new SimpleStringProperty(substitutePrefix(resource.getValue().uri));});
         
        TableColumn<Resource, String> colLabel = new TableColumn<>("Label");
		colLabel.setMinWidth(200);
        colLabel.setCellValueFactory(resource -> {return new SimpleStringProperty(resource.getValue().label);});
        
        TableColumn<Resource, String> colDescr = new TableColumn<>("Description");
		colDescr.setMinWidth(400);
        colDescr.setCellValueFactory(resource -> {return new SimpleStringProperty(cleanupDescription(resource.getValue().descr));});

		tableList.setMinHeight(450);        
        tableList.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        tableList.getColumns().addAll(colUsed, colURI, colLabel, colDescr);
        tableList.setItems(FXCollections.observableArrayList(searchedSubset(searchText)));

        BorderPane pane = new BorderPane();
        pane.setPrefSize(800, 500);
        pane.setMaxHeight(Double.MAX_VALUE);
        pane.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));
        BorderPane.setMargin(line, new Insets(0, 0, PADDING, 0));
        pane.setTop(line);
        pane.setCenter(tableList);
        
        tabList.setContent(pane);

		fieldSearch.setText(searchText);
		fieldSearch.textProperty().addListener((observable, oldValue, newValue) -> 
		{
			tableList.setItems(FXCollections.observableArrayList(searchedSubset(newValue)));
		});
	}

	private void setupTree(String searchText)
	{
		for (Vocabulary.Branch branch : hier.rootBranches) 
		{
			TreeItem<Vocabulary.Branch> item = populateTreeBranch(treeRoot, branch);
			item.setExpanded(true); // open up just the first level
		}
		
		treeView.setShowRoot(false);
		treeView.setCellFactory((p) -> new HierarchyTreeCell());
	
        BorderPane pane = new BorderPane();
        pane.setPrefSize(800, 500);
        pane.setMaxHeight(Double.MAX_VALUE);
        pane.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));
        pane.setCenter(treeView);
        
        tabTree.setContent(pane);
	}
	
	// recursively add a new branch into the tree
	private TreeItem<Vocabulary.Branch> populateTreeBranch(TreeItem<Vocabulary.Branch> parent, Vocabulary.Branch branch)
	{
		TreeItem<Vocabulary.Branch> item = new TreeItem<>(branch);
		parent.getChildren().add(item);
		
		if (usedURI.contains(branch.uri))
		{
			TreeItem<Vocabulary.Branch> look = item.getParent();
			while (look != null)
			{
				look.setExpanded(true);
				look = look.getParent();
			}
		}
		
		for (Vocabulary.Branch child : branch.children) populateTreeBranch(item, child);
		return item;
	}
	
	// manufactures a value from the selected items
	private LookupPanel.Resource[] composeCurrentValue()
	{
		if (tabber.getSelectionModel().getSelectedItem() == tabList)
		{
			List<LookupPanel.Resource> list = tableList.getSelectionModel().getSelectedItems();
			return list.toArray(new LookupPanel.Resource[list.size()]);
		}
		else if (tabber.getSelectionModel().getSelectedItem() == tabTree)
		{
			List<TreeItem<Vocabulary.Branch>> list = treeView.getSelectionModel().getSelectedItems();
			
			List<LookupPanel.Resource> ret = new ArrayList<>();
			for (int n = 0; n < list.size(); n++)
			{
				Vocabulary.Branch branch = list.get(n).getValue();
				
				if (multi && usedURI.contains(branch.uri)) continue;
				
				ret.add(new Resource(branch.uri, branch.label, vocab.getDescr(branch.uri)));
			}
			return ret.toArray(new LookupPanel.Resource[ret.size()]);
		}
		return null;
	}

	// returns a subset of the resources which matches the search text (or all if blank)
	private List<Resource> searchedSubset(String searchText)
	{
		if (searchText.length() == 0) return resources;
		
		String searchLC = searchText.toLowerCase();
		
		List<Resource> subset = new ArrayList<>();
		for (Resource res : resources)
		{
			if (res.label.toLowerCase().indexOf(searchLC) >= 0 || res.uri.toLowerCase().indexOf(searchLC) >= 0 ||
				res.descr.toLowerCase().indexOf(searchLC) >= 0) subset.add(res);
		}
		return subset;
	}

	// when switching tabs: update selection to match	
	private void syncSelectionListToTree()
	{
		if (tableList.getSelectionModel().getSelectedIndex() < 0) return; // nothing selected: do not disturb

		Set<String> selected = new HashSet<>();
		for (Resource res : tableList.getSelectionModel().getSelectedItems()) selected.add(res.uri);
		
		treeView.getSelectionModel().clearSelection();
		
		List<TreeItem<Vocabulary.Branch>> stack = new ArrayList<>();
		stack.add(treeRoot);
		List<TreeItem<Vocabulary.Branch>> toSelect = new ArrayList<>();
		while (stack.size() > 0)
		{
			TreeItem<Vocabulary.Branch> item = stack.remove(0);
			
			if (selected.contains(item.getValue().uri))
			{
				toSelect.add(item);
				for (TreeItem<Vocabulary.Branch> look = item.getParent(); look != null; look = look.getParent()) look.setExpanded(true);
				if (!multi) break; // since a URI can appear twice in the tree due to multiple inheritance, this check is necessary
			}
			
			for (TreeItem<Vocabulary.Branch> child : item.getChildren()) stack.add(child);
		}
		
		for (int n = 0; n < toSelect.size(); n++)
		{
			TreeItem<Vocabulary.Branch> item = toSelect.get(n);
			int row = treeView.getRow(item);
			treeView.getSelectionModel().select(row);
			if (n == 0) treeView.scrollTo(row);
		}
	}
	private void syncSelectionTreeToList()
	{
		if (treeView.getSelectionModel().getSelectedIndex() < 0) return; // nothing selected: do not disturb
	
		Map<String, Integer> uriToIndex = new HashMap<>();
		List<Resource> resList = tableList.getItems();
		for (int n = 0; n < resList.size(); n++) uriToIndex.put(resList.get(n).uri, n);
		
		tableList.getSelectionModel().clearSelection();
		int scrollTo = -1;
		for (TreeItem<Vocabulary.Branch> item : treeView.getSelectionModel().getSelectedItems())
		{
			Integer idx = uriToIndex.get(item.getValue().uri);
			if (idx != null)
			{
				tableList.getSelectionModel().select(idx);
				if (scrollTo < 0) scrollTo = idx;
			}
		}
		
		if (scrollTo >= 0) tableList.scrollTo(scrollTo);
	}
			
	// switches shorter prefixes for display convenience
	private final String[] SUBST = 
    {
    	"obo:", "http://purl.obolibrary.org/obo/",
    	"bao:", "http://www.bioassayontology.org/bao#",
    	"bat:",	"http://www.bioassayontology.org/bat#",
    	"uo:",	"http://purl.org/obo/owl/UO#"
    };
	private String substitutePrefix(String uri)
	{
		for (int n = 0; n < SUBST.length; n += 2)
		{
			if (uri.startsWith(SUBST[n + 1])) return SUBST[n] + uri.substring(SUBST[n + 1].length());
		}
		return uri;
	}
	
	private String cleanupDescription(String descr)
	{
		return descr.replaceAll("\n", " ");
	}
}
