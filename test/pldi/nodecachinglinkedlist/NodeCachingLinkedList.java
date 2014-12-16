package pldi.nodecachinglinkedlist;


import pldi.nodecachinglinkedlist.LinkedListNode;


/**
 * @j2daType
 *//*@ nullable_by_default @*/public class NodeCachingLinkedList {

    public pldi.nodecachinglinkedlist.LinkedListNode header;

    public pldi.nodecachinglinkedlist.LinkedListNode firstCachedNode;

    public int maximumCacheSize;

    public int cacheSize;

    public int size;

    public int DEFAULT_MAXIMUM_CACHE_SIZE;

    public int modCount;

    public NodeCachingLinkedList() {
        this.header = new pldi.nodecachinglinkedlist.LinkedListNode();
        this.header.next = this.header;
        this.header.previous = this.header;
        this.firstCachedNode = null;
        this.size = 0;
        this.cacheSize = 0;
        this.DEFAULT_MAXIMUM_CACHE_SIZE = 3;
        this.maximumCacheSize = 3;
        this.modCount = 0;
    }

/*@
	  @ invariant this.header!=null &&
	  @           this.header.next!=null &&
	  @           this.header.previous!=null &&
	  @
	  @           (\forall LinkedListNode n; \reach(this.header,LinkedListNode,next).has(n); n!=null && n.previous!=null && n.previous.next==n && n.next!=null && n.next.previous==n ) &&
	  @
	  @           this.size + 1 == \reach(this.header,LinkedListNode,next).int_size() &&
	  @           this.size>=0;
	  @
	  @ invariant (\forall LinkedListNode m; \reach(this.firstCachedNode, LinkedListNode, next).has(m);
	  @                                   \reach(m.next, LinkedListNode, next).has(m)==false &&
	  @                                   m.previous==null 
	  @                                   );
	  @
	  @ invariant this.cacheSize <= this.maximumCacheSize;
	  @
	  @ invariant this.DEFAULT_MAXIMUM_CACHE_SIZE == 3;
	  @
	  @ invariant this.cacheSize == \reach(this.firstCachedNode, LinkedListNode, next).int_size();
	  @*//*@
	  @  requires index>=0 && index<this.size;
	  @  requires this.maximumCacheSize == this.DEFAULT_MAXIMUM_CACHE_SIZE;
	  @  ensures this.size == \old(this.size) - 1;
	  @  ensures \old(cacheSize) < maximumCacheSize ==> cacheSize == \old(cacheSize) + 1;
	  @  ensures this.modCount == \old(this.modCount) + 1;
	  @  ensures (index == 0 && size > 0) ==> \result == \old(this.header.next.value);
	  @  ensures (index == 1 && size > 1) ==> \result == \old(this.header.next.next.value);
	  @  ensures (index == 2 && size > 2) ==> \result == \old(this.header.next.next.next.value);
	  @  ensures (\forall LinkedListNode n; \reach(header, LinkedListNode, next).has(n); \old(\reach(header, LinkedListNode, next)).has(n));
	  @  ensures (\exists LinkedListNode n; \old(\reach(header, LinkedListNode, next)).has(n); \reach(header, LinkedListNode, next).has(n) == false);
	  @  ensures (\forall LinkedListNode n; \old(\reach(firstCachedNode, LinkedListNode, next)).has(n); \reach(firstCachedNode, LinkedListNode, next).has(n));
	  @  ensures (\forall LinkedListNode n; \old(\reach(firstCachedNode, LinkedListNode, next)).has(n); n.previous == null);
	  @  ensures this.maximumCacheSize == this.DEFAULT_MAXIMUM_CACHE_SIZE;
	  @  signals (RuntimeException e) false;
	  @*/
    public /*@nullable@*/java.lang.Object remove( final int index ) {
    	LinkedListNode node = null;
    	if (index < 0) {
    		throw new java.lang.RuntimeException();
    	}
    	if (index == this.size) {
    		throw new java.lang.RuntimeException();
    	}
    	if (index > this.size) {
    		throw new java.lang.IndexOutOfBoundsException();
    	}
    	if (index < this.size / 2) {
    		node = this.header.next;
    		for (int currentIndex = 0; currentIndex < index; currentIndex++) { //mutGenLimit 2
    			node = node.next;
    		}
    	} else {
    		node = this.header;
    		for (int currentIndex = size; currentIndex > index; currentIndex--) { //mutGenLimit 2
    			node = node.previous;
    		}
    	}
    	Object oldValue;
    	oldValue = node.value; //mutGenLimit 1
    	node.previous.next = node.next; //mutGenLimit 1
    	node.next.previous = node.previous; //mutGenLimit 1
    	this.size = this.size - 1; //mutGenLimit 1
    	this.modCount = this.modCount + 1; //mutGenLimit 1
    	if (this.cacheSize < this.maximumCacheSize) { //mutGenLimit 1
    		LinkedListNode nextCachedNode; //mutGenLimit 1
    		nextCachedNode = this.firstCachedNode; //mutGenLimit 1
    		node.previous = null; //mutGenLimit 1
    		node.next = nextCachedNode; //mutGenLimit 1
    		node.value = null; //mutGenLimit 1
    		this.firstCachedNode = node; //mutGenLimit 1
    		this.cacheSize = this.cacheSize + 1; //mutGenLimit 1
    	}
    	return oldValue; //mutGenLimit 1
    }

/*@ requires true;
      @ ensures size == \old(size) + 1;
      @ ensures modCount == \old(modCount) + 1;
      @ ensures ( \forall LinkedListNode n; \old(\reach(header, LinkedListNode, next)).has(n); \reach(header, LinkedListNode, next).has(n));
      @ ensures ( \forall LinkedListNode n; \reach(header, LinkedListNode, next).has(n) && n != header.next; \old(\reach(header, LinkedListNode, next)).has(n) );
      @ ensures ( header.next.value == o );
      @ ensures \result == true;
      @*/    
    public boolean addFirst( java.lang.Object o ) {
    	LinkedListNode newNode = new LinkedListNode();
    	newNode.value = o; //mutGenLimit 2
    	LinkedListNode insertBeforeNode = this.header.next;
    	newNode.next = insertBeforeNode; //mutGenLimit 2
    	newNode.previous = insertBeforeNode.previous; //mutGenLimit 2
    	insertBeforeNode.previous.next = newNode; //mutGenLimit 2
    	insertBeforeNode.previous = newNode; //mutGenLimit 2
    	this.size++;
    	this.modCount++;
    	return true;
    }

      /*@ 
      @ requires true;
      @ ensures \result == true <==> (\exists LinkedListNode n; \reach(header, LinkedListNode, next).has(n) && n != header; n.value == arg);
      @*/
    public /*@ pure @*/boolean contains( /*@ nullable @*/java.lang.Object arg ) {
    	LinkedListNode node = this.header.next; //mutGenLimit 2
    	while (node != this.header) {
    		if (node.value == arg) {
    			return true;
    		}
    		node = node.next; //mutGenLimit 2
    	}
    	return false;
    }

}
