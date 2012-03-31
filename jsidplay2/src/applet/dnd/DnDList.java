package applet.dnd;

import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import javax.swing.DefaultListModel;
import javax.swing.JList;

/**
 * An extension of {@link javax.swing.JList} that supports drag and drop to
 * rearrange its contents and to move objects in and out of the list. The
 * objects in the list will be passed either as a String by calling the object's
 * <tt>toString()</tt> object, or if your drag and drop target accepts the
 * {@link TransferableObject#DATA_FLAVOR} data flavor then the actual object
 * will be passed.
 * 
 * <p>
 * I'm releasing this code into the Public Domain. Enjoy.
 * </p>
 * <p>
 * <em>Original author: Robert Harder, rharder@usa.net</em>
 * </p>
 * 
 * @author Robert Harder
 * @author rharder@usa.net
 * @version 1.1
 */
class DnDList extends JList implements DropTargetListener, DragSourceListener, DragGestureListener {
	private DragSource dragSource = null;

	protected int sourceIndex = -1;

	/**
	 * Constructs a default {@link DnDList} using a
	 * {@link javax.swing.DefaultListModel}.
	 * 
	 * @since 1.1
	 */
	public DnDList() {
		super(new DefaultListModel());
		initComponents();
	}

	private void initComponents() {
		new DropTarget(this, this);
		dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer(this,
				DnDConstants.ACTION_MOVE, this);
	}

	/* DragGesture API */
	public void dragGestureRecognized(final DragGestureEvent event) {
		final Object selected = getSelectedValue();
		if (selected == null) {
			return;
		}

		sourceIndex = getSelectedIndex();
		final Transferable transfer = new TransferableObject(
				new TransferableObject.Fetcher() {
					/**
					 * This will be called when the transfer data is
					 * requested at the very end. At this point we can
					 * remove the object from its original place in the
					 * list.
					 */
					public Object getObject() {
						((DefaultListModel) getModel())
						.remove(sourceIndex);
						return selected;
					}
				}
		);
		
		dragSource.startDrag(event,
				java.awt.dnd.DragSource.DefaultLinkDrop, transfer, this);
	}

	/* DragSource API */
	public void dragDropEnd(final DragSourceDropEvent evt) {
	}
	public void dragEnter(final DragSourceDragEvent evt) {
	}
	public void dragExit(final DragSourceEvent evt) {
	}
	public void dragOver(final DragSourceDragEvent evt) {
	}
	public void dropActionChanged(final DragSourceDragEvent evt) {
	}
	
	/* DropTarget API */
	public void dragEnter(final DropTargetDragEvent evt) {
		evt.acceptDrag(java.awt.dnd.DnDConstants.ACTION_MOVE);
	}
	public void dragExit(final DropTargetEvent evt) {
	}
	public void dragOver(final DropTargetDragEvent evt) {
	}
	public void dropActionChanged(final DropTargetDragEvent evt) {
		evt.acceptDrag(java.awt.dnd.DnDConstants.ACTION_MOVE);
	}
	public void drop(final DropTargetDropEvent evt) {
		final Transferable transferable = evt.getTransferable();

		// If it's our native TransferableObject, use that
		if (transferable.isDataFlavorSupported(TransferableObject.DATA_FLAVOR)) {
			evt.acceptDrop(java.awt.dnd.DnDConstants.ACTION_MOVE);
			Object obj = null;
			try {
				obj = transferable.getTransferData(TransferableObject.DATA_FLAVOR);
			} // end try
			catch (final java.awt.datatransfer.UnsupportedFlavorException e) {
				e.printStackTrace();
			} // end catch
			catch (final java.io.IOException e) {
				e.printStackTrace();
			} // end catch

			if (obj != null) {
				// See where in the list we dropped the element.
				final int dropIndex = locationToIndex(evt.getLocation());
				final DefaultListModel model = (DefaultListModel) getModel();

				if (dropIndex < 0) {
					model.addElement(obj);
				} else if (sourceIndex >= 0 && dropIndex > sourceIndex) {
					model.add(dropIndex - 1, obj);
				} else {
					model.add(dropIndex, obj);
				}
			} else {
				evt.rejectDrop();
			}
		}
		else {
			evt.rejectDrop();
		}
	}
}
