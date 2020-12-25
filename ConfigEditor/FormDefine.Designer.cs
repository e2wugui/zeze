
namespace ConfigEditor
{
    partial class FormDefine
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.define = new System.Windows.Forms.DataGridView();
            this.Bean = new System.Windows.Forms.DataGridViewCheckBoxColumn();
            this.VarName = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.VarType = new System.Windows.Forms.DataGridViewComboBoxColumn();
            this.VarValue = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.VarForeign = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.VarProperties = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.VarComment = new System.Windows.Forms.DataGridViewTextBoxColumn();
            ((System.ComponentModel.ISupportInitialize)(this.define)).BeginInit();
            this.SuspendLayout();
            // 
            // define
            // 
            this.define.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
            | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.define.ColumnHeadersHeightSizeMode = System.Windows.Forms.DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            this.define.Columns.AddRange(new System.Windows.Forms.DataGridViewColumn[] {
            this.Bean,
            this.VarName,
            this.VarType,
            this.VarValue,
            this.VarForeign,
            this.VarProperties,
            this.VarComment});
            this.define.Location = new System.Drawing.Point(3, 1);
            this.define.Name = "define";
            this.define.RowHeadersWidth = 82;
            this.define.RowTemplate.Height = 37;
            this.define.Size = new System.Drawing.Size(1740, 904);
            this.define.TabIndex = 0;
            // 
            // Bean
            // 
            this.Bean.HeaderText = "Bean";
            this.Bean.MinimumWidth = 10;
            this.Bean.Name = "Bean";
            this.Bean.Width = 200;
            // 
            // VarName
            // 
            this.VarName.HeaderText = "列名";
            this.VarName.MinimumWidth = 10;
            this.VarName.Name = "VarName";
            this.VarName.Width = 200;
            // 
            // VarType
            // 
            this.VarType.HeaderText = "类型";
            this.VarType.MinimumWidth = 10;
            this.VarType.Name = "VarType";
            this.VarType.Width = 150;
            // 
            // VarValue
            // 
            this.VarValue.HeaderText = "引用";
            this.VarValue.MinimumWidth = 10;
            this.VarValue.Name = "VarValue";
            this.VarValue.Width = 200;
            // 
            // VarForeign
            // 
            this.VarForeign.HeaderText = "Foreign";
            this.VarForeign.MinimumWidth = 10;
            this.VarForeign.Name = "VarForeign";
            this.VarForeign.Width = 200;
            // 
            // VarProperties
            // 
            this.VarProperties.HeaderText = "属性";
            this.VarProperties.MinimumWidth = 10;
            this.VarProperties.Name = "VarProperties";
            this.VarProperties.Resizable = System.Windows.Forms.DataGridViewTriState.True;
            this.VarProperties.SortMode = System.Windows.Forms.DataGridViewColumnSortMode.NotSortable;
            this.VarProperties.Width = 300;
            // 
            // VarComment
            // 
            this.VarComment.HeaderText = "注释";
            this.VarComment.MinimumWidth = 10;
            this.VarComment.Name = "VarComment";
            this.VarComment.Width = 300;
            // 
            // FormDefine
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(12F, 24F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(1742, 905);
            this.Controls.Add(this.define);
            this.Name = "FormDefine";
            this.Text = "Bean 结构定义";
            ((System.ComponentModel.ISupportInitialize)(this.define)).EndInit();
            this.ResumeLayout(false);

        }

        #endregion

        private System.Windows.Forms.DataGridView define;
        private System.Windows.Forms.DataGridViewCheckBoxColumn Bean;
        private System.Windows.Forms.DataGridViewTextBoxColumn VarName;
        private System.Windows.Forms.DataGridViewComboBoxColumn VarType;
        private System.Windows.Forms.DataGridViewTextBoxColumn VarValue;
        private System.Windows.Forms.DataGridViewTextBoxColumn VarForeign;
        private System.Windows.Forms.DataGridViewTextBoxColumn VarProperties;
        private System.Windows.Forms.DataGridViewTextBoxColumn VarComment;
    }
}