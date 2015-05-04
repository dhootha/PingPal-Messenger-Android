
package io.pingpal.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.CursorLoader;

public class DatabaseCursorLoader extends CursorLoader {

    @SuppressWarnings("unused")
	private static final String TAG = DatabaseCursorLoader.class.getSimpleName();

    @SuppressWarnings("unused")
	private String[] mProjection;

    private String mSelection;

    private String[] mSelectionArgs;

    private String mSortOrder;

    private String mRawQuery;

    private boolean isRawQuery = false;

    public DatabaseCursorLoader(Context context, String rawQuery, String[] selectionArgs) {
        super(context);

        this.mRawQuery = rawQuery;
        isRawQuery = true;
        this.mSelectionArgs = selectionArgs;
    }

    public DatabaseCursorLoader(Context context, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        super(context);

        this.mProjection = projection;
        this.mSelection = selection;
        this.mSelectionArgs = selectionArgs;
        this.mSortOrder = sortOrder;
        isRawQuery = false;

    }

    @Override
    public Cursor loadInBackground() {
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor;

        if (isRawQuery) {
            cursor = db.rawQuery(mRawQuery, mSelectionArgs);
        } else {
            cursor = db.query(DatabaseHelper.TABLE_MESSAGES, mSelectionArgs, mSelection,
                    mSelectionArgs, null, null, mSortOrder);
        }

        cursor.getCount();

        db.close();
        return cursor;
    }

}
