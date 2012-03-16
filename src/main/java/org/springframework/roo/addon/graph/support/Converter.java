package org.springframework.roo.addon.graph.support;

import java.util.Iterator;

abstract class Converter<T, S> implements Iterable<T>
{
    private final Iterable<S> source;

    public Converter( Iterable<S> source )
    {
        this.source = source;
    }

    public Iterator<T> iterator()
    {
        return new Iterator<T>()
        {
            final Iterator<S> source = Converter.this.source.iterator();

            public boolean hasNext()
            {
                return source.hasNext();
            }

            public T next()
            {
                return convert( source.next() );
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    abstract T convert( S source );
}
